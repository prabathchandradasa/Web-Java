/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author MSI
 */
@WebServlet(name = "VerifyPayment", urlPatterns = {"/VerifyPayment"})
public class VerifyPayment extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String merchant_id=request.getParameter("merchant_id");
        String order_id =request.getParameter("order_id");
        String payhere_amount=request.getParameter("payhere_amount");
        String payhere_currency=request.getParameter("payhere_currency");
        String status_code=request.getParameter("status_code");
        String md5sig=request.getParameter("md5sig");
        
        String merchant_secret="";
        String merchant_secret_md5hash="";
        
    }
    
}
