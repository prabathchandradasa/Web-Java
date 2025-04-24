package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dto.User_DTO;
import entity.Address;
import entity.Cart;
import entity.City;
import entity.Order_Item;
import entity.Order_Status;
import entity.Product;
import entity.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.HibernateUtil;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import model.PayHere;

/**
 *
 * @author MSI
 */
@WebServlet(name = "Checkout", urlPatterns = {"/Checkout"})
public class Checkout extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Gson gson = new Gson();

        JsonObject requestjsonObject = gson.fromJson(request.getReader(), JsonObject.class);

        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("success", false);

        HttpSession httpSession = request.getSession();

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        boolean isCurrentAddress = requestjsonObject.get("isCurrentAddress").getAsBoolean();
        String first_name = requestjsonObject.get("first_name").getAsString();
        String last_name = requestjsonObject.get("last_name").getAsString();
        String city_id = requestjsonObject.get("city").getAsString();
        String address1 = requestjsonObject.get("address1").getAsString();
        String address2 = requestjsonObject.get("address2").getAsString();
        String postal_code = requestjsonObject.get("postal_code").getAsString();
        String mobile = requestjsonObject.get("mobile").getAsString();

        if (httpSession.getAttribute("user") != null) {
            //user signed in
            User_DTO user_DTO = (User_DTO) httpSession.getAttribute("user");
            Criteria criteria1 = session.createCriteria(User.class);
            criteria1.add(Restrictions.eq("email", user_DTO.getEmail()));
            User user = (User) criteria1.uniqueResult();

            if (isCurrentAddress) {
                //get current address
                Criteria criteria2 = session.createCriteria(Address.class);
                criteria2.add(Restrictions.eq("user", user));
                criteria2.addOrder(Order.desc("id"));
                criteria2.setMaxResults(1);

                if (criteria2.list().isEmpty()) {
                    //current address not found.please create a new address
                    responseJsonObject.addProperty("message", "current address not found.please create a new address");

                } else {
                    //current address found
                    Address address = (Address) criteria2.list().get(0);

                    //***Complete the checkout process
                    saveOrders(session, transaction, user, address, responseJsonObject);
                }

            } else {
                //craete new address
                if (first_name.isEmpty()) {
                    responseJsonObject.addProperty("message", "Please fill first name");
                } else if (last_name.isEmpty()) {
                    responseJsonObject.addProperty("message", "Please fill last name");
                } else if (!Validations.isInteger(city_id)) {
                    responseJsonObject.addProperty("message", "Invalid City");
                } else {
                    //Check City From DB
                    Criteria criteria3 = session.createCriteria(City.class);
                    criteria3.add(Restrictions.eq("id", Integer.parseInt(city_id)));

                    if (criteria3.list().isEmpty()) {
                        responseJsonObject.addProperty("message", "Invalid City Selected");
                    } else {
                        City city = (City) criteria3.list().get(0);

                        if (address1.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill address line 1");

                        } else if (address2.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill address line 2");

                        } else if (postal_code.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill Postal code");

                        } else if (postal_code.length() != 5) {
                            responseJsonObject.addProperty("message", "Invalid Postal code");

                        } else if (!Validations.isInteger(postal_code)) {
                            responseJsonObject.addProperty("message", "Invalid Postal code");

                        } else if (mobile.isEmpty()) {
                            responseJsonObject.addProperty("message", "Please fill mobile");

                        } else if (!Validations.isMobileNumberValid(mobile)) {
                            responseJsonObject.addProperty("message", "Invalid mobile Number");
                        } else {
                            //Create new address

                            Address address = new Address();
                            address.setCity(city);
                            address.setFirst_name(first_name);
                            address.setLast_name(last_name);
                            address.setLine1(address1);
                            address.setLine2(address2);
                            address.setMobile(mobile);
                            address.setPostal_code(postal_code);
                            address.setUser(user);

                            session.save(address);
                            //***Complete the checkout process
                            saveOrders(session, transaction, user, address, responseJsonObject);
                        }
                    }
                }
            }

        } else {
            //User not signed in
            responseJsonObject.addProperty("message", "User not signed in");
        }
        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(responseJsonObject));
    }

    private void saveOrders(Session session, Transaction transaction, User user, Address address, JsonObject responseJsonObject) {
        try {
            //Create Orders in DB
            entity.Orders order = new entity.Orders();
            order.setAddress(address);
            order.setDate_time(new Date());
            order.setUser(user);
            session.save(order);
            int order_id = (int) session.save(order);

            //Get cart Items
            Criteria criteria4 = session.createCriteria(Cart.class);
            criteria4.add(Restrictions.eq("user", user));
            List<Cart> cartList = criteria4.list();

            //get order status (5.paiment pending) from DB
            Order_Status order_Status = (Order_Status) session.get(Order_Status.class, 5);

            //Create Orders Item in DB
            double amount = 0;
            String items="";
            for (Cart cartItem : cartList) {
//calculate amount
                amount += cartItem.getQty() * cartItem.getProduct().getPrice();
                if (address.getCity().getId() == 1) {
                    amount += 1000;
                } else {
                    amount += 2500;
                }
                
                //get item deatails
                items += cartItem.getProduct().getTitle()+" x"+cartItem.getQty();
                //get item deatails
                
                //Get Product
                Product product = cartItem.getProduct();

                Order_Item order_Item = new Order_Item();
                order_Item.setOrder(order);
                order_Item.setOrder_status(order_Status);
                order_Item.setProduct(product);
                order_Item.setQty(cartItem.getQty());
                session.save(order_Item);

                //update Product Qty in DB
                product.setQty(product.getQty() - cartItem.getQty());
                session.update(product);

                //delete cart Item from db
                session.delete(cartItem);
            }
            transaction.commit();

            //Start : Set payment Data
            String merchant_id="1221326";
            String formatedAmount=new DecimalFormat("0.00").format(amount);
            String currency="LKR";
            String merchantSecret="Mjg0NjM4ODI3ODE1MDI0MjU3MjY3MTg4ODM4MTkyOTI0Njc2NzQ0";
            String merchantSecretMd5Hash=PayHere.generateMD5(merchantSecret);
            
            JsonObject payhere = new JsonObject();
            payhere.addProperty("merchant_id", merchant_id);
            payhere.addProperty("return_url", "");
            payhere.addProperty("cancel_url", "");
            payhere.addProperty("notify_url", "");
            payhere.addProperty("first_name", user.getFirst_name());
            payhere.addProperty("last_name", user.getLast_name());
            payhere.addProperty("email", user.getEmail());
            payhere.addProperty("phone", "");
            payhere.addProperty("address", "");
            payhere.addProperty("city", "");
            payhere.addProperty("country", "");
            payhere.addProperty("order_id", String.valueOf(order_id));
            payhere.addProperty("items",items);
            payhere.addProperty("currency", currency);
            payhere.addProperty("amount",formatedAmount);
             payhere.addProperty("sandbox", true);
             
              String md5Hash=PayHere.generateMD5(merchant_id+order_id+formatedAmount+currency+merchantSecretMd5Hash);
            payhere.addProperty("hash", md5Hash);
           
            
          
            //End : Set payment Data

            responseJsonObject.addProperty("success", true);
            responseJsonObject.addProperty("message", "Checkout Complete");
            
            Gson gson=new Gson();
            responseJsonObject.add("payhereJson",gson.toJsonTree(payhere));

        } catch (Exception e) {
            transaction.rollback();
        }
    }
}
