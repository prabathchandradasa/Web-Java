package controller;

import com.google.gson.Gson;
import dto.Cart_DTO;
import dto.Response_DTO;
import dto.User_DTO;
import entity.Cart;
import entity.Product;
import entity.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.HibernateUtil;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "AddToCart", urlPatterns = {"/AddToCart"})
public class AddToCart extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Gson gson = new Gson();
        Response_DTO response_DTO = new Response_DTO();

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();

        try {

            String id = request.getParameter("id");
            String qty = request.getParameter("qty");

            if (!Validations.isInteger(id)) {
                //Product not found
                response_DTO.setContent("Product not found");
            } else if (!Validations.isInteger(qty)) {
                //invalid quentity
                response_DTO.setContent("invalid quentity");
            } else {

                int productId = Integer.parseInt(id);
                int productQty = Integer.parseInt(qty);

                if (productQty <= 0) {
                    response_DTO.setContent("quentity must be grater than 0");
                    //quentity must be grater than 0
                } else {

                    Product product = (Product) session.load(Product.class, productId);

                    if (product != null) {
//product found
                        if (request.getSession().getAttribute("user") != null) {
                            //DB cart

                            User_DTO user_DTO = (User_DTO) request.getSession().getAttribute("user");
//get User
                            Criteria criteria1 = session.createCriteria(User.class);
                            criteria1.add(Restrictions.eq("email", user_DTO.getEmail()));
                            User user = (User) criteria1.uniqueResult();

                            //check in db cart
                            Criteria criteria2 = session.createCriteria(Cart.class);
                            criteria2.add(Restrictions.eq("user", user));
                            criteria2.add(Restrictions.eq("product", product));

                            if (criteria2.list().isEmpty()) {
                                //item not found in cart

                                if (productQty <= product.getQty()) {
                                    //add product into cart

                                    Cart cart = new Cart();
                                    cart.setProduct(product);
                                    cart.setQty(productQty);
                                    cart.setUser(user);
                                    session.save(cart);
                                    transaction.commit();

                                    response_DTO.setSuccess(true);
                                    response_DTO.setContent("Product Added to the cart");

                                } else {
                                    //quentity not available
                                    response_DTO.setContent("quentity not available");
                                }

                            } else {
                                //item already found in cart

                                Cart cartItem = (Cart) criteria2.uniqueResult();

                                if ((cartItem.getQty() + productQty) <= product.getQty()) {
                                    cartItem.setQty(cartItem.getQty() + productQty);
                                    session.update(cartItem);
                                    transaction.commit();

                                    response_DTO.setSuccess(true);
                                    response_DTO.setContent("Product Added to the cart");

                                } else {
                                    //can't update your cart.Quentity not available 

                                    response_DTO.setContent("cant");
                                }

                            }

                        } else {
                            //Session Cart

                            HttpSession httpSession = request.getSession();

                            if (httpSession.getAttribute("sessionCart") != null) {
                                //session cart found

                                ArrayList<Cart_DTO> sessionCart = (ArrayList<Cart_DTO>) httpSession.getAttribute("sessionCart");

                                Cart_DTO foundCart_DTO = null;

                                for (Cart_DTO cart_DTO : sessionCart) {

                                    if (cart_DTO.getProduct().getId() == product.getId()) {
                                        foundCart_DTO = cart_DTO;
                                        break;
                                    }

                                }

                                if (foundCart_DTO != null) {
                                    //product found

                                    if ((foundCart_DTO.getQty() + productQty) <= product.getQty()) {
                                        //update qty

                                        foundCart_DTO.setQty(foundCart_DTO.getQty() + productQty);
                                        response_DTO.setSuccess(true);
                                        response_DTO.setContent("Cart Item updated");
                                    } else {
                                        //quantity not available

                                        response_DTO.setContent("quantity not available");
                                    }

                                } else {
                                    //product not found

                                    if (productQty <= product.getQty()) {
                                        //add to session cart
                                        Cart_DTO cart_DTO = new Cart_DTO();
                                        cart_DTO.setProduct(product);
                                        cart_DTO.setQty(productQty);
                                        sessionCart.add(cart_DTO);

                                        response_DTO.setSuccess(true);
                                        response_DTO.setContent("Product Added to the cart");

                                    } else {
                                        //quentity not available

                                        response_DTO.setContent("quentity not available");
                                    }
                                }

                            } else {
                                //session cart not found

                                if (productQty <= product.getQty()) {
                                    //add to session cart

                                    ArrayList<Cart_DTO> sessionCart = new ArrayList<>();

                                    Cart_DTO cart_DTO = new Cart_DTO();
                                    cart_DTO.setProduct(product);
                                    cart_DTO.setQty(productQty);
                                    sessionCart.add(cart_DTO);

                                    httpSession.setAttribute("sessionCart", sessionCart);
                                   
                                    response_DTO.setSuccess(true);
                                    response_DTO.setContent("Product Added to the cart");

                                } else {
                                    //quentity not available
                                    response_DTO.setContent("Product not found");
                                }
                            }
                        }

                    } else {
                        //product not found

                        response_DTO.setContent("Product not found");
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(response_DTO));
        session.close();

    }

}
