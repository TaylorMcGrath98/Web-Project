package controllers;

import controllers.security.*;

import play.mvc.*;
import play.data.*;
import play.db.ebean.Transactional;
import play.api.Environment;

import play.mvc.Http.*;
import play.mvc.Http.MultipartFormData.FilePart;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import play.Logger;

// File upload and image editing dependencies
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;


// Import models and views
import models.users.*;
import models.products.*;

import views.html.productAdmin.*;


/* - Docs -
http://superuser.com/questions/163818/how-to-install-rmagick-on-ubuntu-10-04
http://im4java.sourceforge.net/
*/

// Authenticate user
@Security.Authenticated(Secured.class)
// Authorise user (check if admin)
@With(CheckIfAdmin.class)

public class AdminProductCtrl extends Controller {

    /** Dependency Injection **/

    /** http://stackoverflow.com/questions/15600186/play-framework-dependency-injection **/
    private FormFactory formFactory;

    /** http://stackoverflow.com/a/37024198 **/
    private Environment env;

    /** http://stackoverflow.com/a/10159220/6322856 **/
    @Inject
    public AdminProductCtrl(Environment e, FormFactory f) {
        this.env = e;
        this.formFactory = f;
    }



	
    // Get a user - if logged in email will be set in the session
	private User getCurrentUser() {
		User u = User.getLoggedIn(session().get("email"));
		return u;
	}
    
    public Result index() {
        return redirect(controllers.routes.AdminProductCtrl.listProducts(0));
    }

	// Get a list of products
    // If cat parameter is 0 then return all products
    // Otherwise return products for a category (by id)
    @Transactional
    public Result listProducts(Long cat) {
        // Get list of categories in ascending order
        List<Category> categories = Category.find.query().where().orderBy("name asc").findList();
        // Instantiate products, an Array list of products			
        List<Product> products = new ArrayList<Product>();

         if (cat == 0) {
            // Get the list of ALL products with filter
            products = Product.findAll("");
        }
        else {
            // Get products for the selected category and filter (search field)
            products = Product.findFilter(cat, "");
        }
        // Render the list products view, passing parameters
        // categories and products lists
        // current user - if one is logged in
        return ok(listProducts.render(env, categories, products, getCurrentUser()));
    }
    
    // Load the add product view
    // Display an empty form in the view
    @Transactional
    public Result addProduct() {   
        // Instantiate a form object based on the Product class
        Form<Product> addProductForm = formFactory.form(Product.class);
        // Render the Add Product View, passing the form object
        return ok(addProduct.render(addProductForm, getCurrentUser()));
    }

    // Handle the form data when a new product is submitted
    @Transactional
    public Result addProductSubmit() {

        String saveImageMsg;

        // Create a product form object (to hold submitted data)
        // 'Bind' the object to the submitted form (this copies the filled form)
        Form<Product> newProductForm = formFactory.form(Product.class).bindFromRequest();

        // Check for errors (based on Product class annotations)	
        if(newProductForm.hasErrors()) {
            // Display the form again
            return badRequest(addProduct.render(newProductForm, getCurrentUser()));
        }
     
        Product newProduct = newProductForm.get();
        
        // Save product now to set id (needed to update manytomany)
        newProduct.save();
        
        // Get category ids (checked boxes from form)
        // Find category objects and set categories list for this product
        for (Long cat : newProduct.getCatSelect()) {
            newProduct.getCategories().add(Category.find.byId(cat));
        }

        // Update the new Product to save categories
        newProduct.update();

        // Get image data
        MultipartFormData data = request().body().asMultipartFormData();
        FilePart image = data.getFile("upload");
        
        // Save the image file
        saveImageMsg = saveFile(newProduct.getId(), image);

        // Set a success message in temporary flash
        flash("success", "Product " + newProduct.getName() + " has been created" + " " + saveImageMsg);
            
        // Redirect to the admin home
        return redirect(controllers.routes.AdminProductCtrl.index());
    }
        
    // Update a product by ID
    // called when edit button is pressed
    @Transactional
    public Result updateProduct(Long id) {
        // Retrieve the product by id
        // Create a form based on the Product class
        Product p = Product.find.byId(id);
        Form<Product> productForm = formFactory.form(Product.class).fill(p);
        // Render the updateProduct view
        // pass the id and form as parameters
        return ok(updateProduct.render(id, productForm, User.getLoggedIn(session().get("email"))));		
    }


    // Handle the form data when an updated product is submitted
    @Transactional
    public Result updateProductSubmit(Long id) {

        String saveImageMsg;

        // Create a product form object (to hold submitted data)
        // 'Bind' the object to the submitted form (this copies the filled form)
        Form<Product> updateProductForm = formFactory.form(Product.class).bindFromRequest();

        // Check for errors (based on Product class annotations)	
        if(updateProductForm.hasErrors()) {
            // Display the form again
            return badRequest(updateProduct.render(id, updateProductForm, getCurrentUser()));
        }
        
        // Update the Product (using its ID to select the existing object))
        Product p = updateProductForm.get();
        p.setId(id);
        
        // Get category ids (checked boxes from form)
        // Find category objects and set categories list for this product
        List<Category> newCats = new ArrayList<Category>();
        for (Long cat : p.getCatSelect()) {
            newCats.add(Category.find.byId(cat));
        }
        p.setCategories(newCats);
        
        // update (save) this product            
        p.update();

        // Get image data
        MultipartFormData data = request().body().asMultipartFormData();
        FilePart image = data.getFile("upload");

        saveImageMsg = saveFile(p.getId(), image);

        // Add a success message to the flash session
        flash("success", "Product " + updateProductForm.get().getName() + " has been updates" + " " + saveImageMsg);
            
        // Return to admin home
        return redirect(controllers.routes.AdminProductCtrl.index());
    }


    // Delete Product
    @Transactional
    public Result deleteProduct(Long id) {
        // Call delete method
        Product.find.ref(id).delete();
        // Add message to flash session 
        flash("success", "Product has been deleted");
        // Redirect home
        return redirect(routes.AdminProductCtrl.index());
    }
    
    // Save an image file
    public String saveFile(Long id, FilePart<File> image) {
        if (image != null) {
            // Get mimetype from image
            String mimeType = image.getContentType();
            // Check if uploaded file is an image
            if (mimeType.startsWith("image/")) {
                // Create file from uploaded image
                File file = image.getFile();
                // create ImageMagick command instance
                ConvertCmd cmd = new ConvertCmd();
                // create the operation, add images and operators/options
                IMOperation op = new IMOperation();
                // Get the uploaded image file
                op.addImage(file.getAbsolutePath());
                // Resize using height and width constraints
                op.resize(300,200);
                // Save the  image
                op.addImage("public/images/productImages/" + id + ".jpg");
                // thumbnail
                IMOperation thumb = new IMOperation();
                // Get the uploaded image file
                thumb.addImage(file.getAbsolutePath());
                thumb.thumbnail(60);
                // Save the  image
                thumb.addImage("public/images/productImages/thumbnails/" + id + ".jpg");
                // execute the operation
                try{
                    cmd.run(op);
                    cmd.run(thumb);
                }
                catch(Exception e){
                    e.printStackTrace();
                }				
                return " and image saved";
            }
        }
        return "image file missing";	
    } 
}
