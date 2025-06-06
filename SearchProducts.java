/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.Category;
import entity.Color;
import entity.Model;
import entity.Product;
import entity.Product_Condition;
import entity.Storage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author MSI
 */
@WebServlet(name = "SearchProducts", urlPatterns = {"/SearchProducts"})
public class SearchProducts extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        Gson gson = new Gson();
        
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("success", false);

        // get request data
        JsonObject requestJsonObject = gson.fromJson(request.getReader(), JsonObject.class);
        
        Session session = HibernateUtil.getSessionFactory().openSession();

        //Search all Product
        Criteria criteria1 = session.createCriteria(Product.class);

        //add category filter
        if (requestJsonObject.has("category_name")) {
           
            //category selected
            String category_name = requestJsonObject.get("category_name").getAsString();

            //Get Category list from DB
            Criteria criteria2 = session.createCriteria(Category.class);
            criteria2.add(Restrictions.eq("name", category_name));
            Category category = (Category) criteria2.uniqueResult();

            //Filter Models by Category list from DB
            Criteria criteria3 = session.createCriteria(Model.class);
            criteria3.add(Restrictions.eq("category", category));
            List<Model> ModelList = criteria3.list();
    
            //Filter products by model list from DB
            criteria1.add(Restrictions.in("model", ModelList));
            
        }
        
        if (requestJsonObject.has("condition_name")) {
            //condition slected
            String condition_name = requestJsonObject.get("condition_name").getAsString();

            //Get Category list from DB
            Criteria criteria4 = session.createCriteria(Product_Condition.class);
            criteria4.add(Restrictions.eq("name", condition_name));
            Product_Condition product_Condition = (Product_Condition) criteria4.uniqueResult();
            
            criteria1.add(Restrictions.eq("product_condition", product_Condition));
        }
        
        if (requestJsonObject.has("color_name")) {
            
            String color_name = requestJsonObject.get("color_name").getAsString();
            //color selecter
            Criteria criteria5 = session.createCriteria(Color.class);
            criteria5.add(Restrictions.eq("name", color_name));
            Color color = (Color) criteria5.uniqueResult();

            //Filter Products by color from db
            criteria1.add(Restrictions.eq("color", color));
        }
        
        if (requestJsonObject.has("storage_value")) {
            
            String storage_value = requestJsonObject.get("storage_value").getAsString();
            //color selecter
            Criteria criteria6 = session.createCriteria(Storage.class);
            criteria6.add(Restrictions.eq("value", storage_value));
            Storage storage = (Storage) criteria6.uniqueResult();

            //Filter Products by color from db
            criteria1.add(Restrictions.eq("storage", storage));
        }
        
        double price_range_start = requestJsonObject.get("price_range_start").getAsDouble();
        double price_range_end = requestJsonObject.get("price_range_end").getAsDouble();
        
        criteria1.add(Restrictions.ge("price", price_range_start));
        criteria1.add(Restrictions.le("price", price_range_end));
        
        String sort_text = requestJsonObject.get("sort_text").getAsString();
        
        if (sort_text.equals("Short by Latest")) {
            criteria1.addOrder(Order.desc("id"));
        } else if (sort_text.equals("Short by Oldest")) {
            criteria1.addOrder(Order.asc("id"));
        } else if (sort_text.equals("Short by Name")) {
            criteria1.addOrder(Order.asc("title"));
        } else if (sort_text.equals("Short by Price")) {
            criteria1.addOrder(Order.asc("price"));
        }
        //get all product count
        responseJsonObject.addProperty("allProductCount", criteria1.list().size());

        //set product range
        int firstResult=requestJsonObject.get("firstResult").getAsInt();
        criteria1.setFirstResult(firstResult);
        criteria1.setMaxResults(6);

        //get product list
        List<Product> productList = criteria1.list();

//        String category_name = requestJsonObject.get("category_name").getAsString();
//        String condition_name = requestJsonObject.get("condition_name").getAsString();
//        String color_name = requestJsonObject.get("color_name").getAsString();
//        String storage_name = requestJsonObject.get("storage_name").getAsString();
//        String price_range_start = requestJsonObject.get("price_range_start").getAsString();
//        String price_range_end = requestJsonObject.get("price_range_end").getAsString();
//        String sort_text = requestJsonObject.get("sort_text").getAsString();
//Remove Users from product
        for (Product product : productList) {
            product.setUser(null);
        }
        
        responseJsonObject.addProperty("success", true);
        responseJsonObject.add("productList", gson.toJsonTree(productList));

        //Send response
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseJsonObject));
    }
    
}
