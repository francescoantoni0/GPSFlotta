package com.example.gpsflotta;

import java.awt.desktop.OpenURIEvent;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "flottaServlet", value = "/flotta-servlet")
public class FlottaServlet extends HttpServlet {
    private Connection con;
    private boolean connected;

    public void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String username = "root", password = "", database = "Flotta", hostname = "jdbc:mysql://localhost:3306/";
            con = DriverManager.getConnection(hostname + database, username, password);
            connected = true;
        } catch (ClassNotFoundException | SQLException e) {
            connected = false;
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/xml");
        if (!connected) {
            response.sendError(500, "DBMS server error!");
            return;
        }
        String reqURL = request.getRequestURL().toString();
        String[] urlSection = reqURL.split("/");
        String name = urlSection[urlSection.length - 1];

        if (name == null) {
            response.sendError(400, "Request syntax error!");
            System.err.println("name = null");
            return;
        }
        if (name.isEmpty()) {
            response.sendError(400, "Request syntax error!");
            System.err.println("name empty");
            return;
        }

        if (!name.startsWith("HISTORY")) {
            response.sendError(400, "Request syntax error!");
            System.err.println("name wrong");
            return;
        }
        String targa, gotData;
        if ((targa = request.getParameter("targa")) == null) {
            response.sendError(400, "Request syntax error!");
            System.err.println("missing targa");
            return;
        }
        if ((gotData = request.getParameter("data")) == null) {
            response.sendError(400, "Request syntax error!");
            System.err.println("missing data");
            return;
        }

        Date data;
        try {
            data = Date.valueOf(LocalDate.parse(gotData, DateTimeFormatter.ofPattern("dd-MM-YY")));
        } catch (DateTimeParseException e) {
            response.sendError(400, "Request syntax error!\n" + e.getMessage());
            System.err.println("wrong data format");
            e.printStackTrace();
            return;
        }

        String query = "SELECT latitudine, longitudine FROM posizioni WHERE veicolo = ? AND data = ?";
        PreparedStatement s;
        ResultSet r;
        try {
            s = con.prepareStatement(query);
            s.setString(1, targa);
            s.setDate(2, data);
            r = s.executeQuery();
        } catch (SQLException e) {
            response.sendError(500, e.getMessage());
            return;
        }
        PrintWriter out = response.getWriter();
        out.println("<?xml version=\"1.1\" encoding=\"UTF-8\"?>");
        out.println("<posizioni targa=\"" + targa + "\">");
        try {
            while (r.next()) {
                out.println("\t<posizione>");
                out.println("\t\t<latitudine>" + r.getDouble("latitudine") + "</latitudine>");
                out.println("\t\t<longitudine>" + r.getDouble("longitudine") + "</longitudine>");
                out.println("\t</posizione>");
            }
        } catch (SQLException e) {
            response.sendError(500, e.getMessage());
            return;
        }
        out.println("</posizioni>");

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!connected) {
            resp.sendError(500, "DBMS server error!");
            return;
        }

        String reqURL = req.getRequestURL().toString();
        String[] urlSection = reqURL.split("/");
        String name = urlSection[urlSection.length - 1];
        if (name == null) {
            resp.sendError(400, "Request syntax error!");
            System.err.println("name = null");
            return;
        }
        if (name.isEmpty()) {
            resp.sendError(400, "Request syntax error! name = " + name);
            System.err.println("name empty");
            return;
        }

        if (!name.startsWith("SAVE")) {
            resp.sendError(400, "Request syntax error!");
            System.err.println("name wrong");
            return;
        }

        String targa, lat, lon;

        if ((targa = req.getParameter("targa")) == null) {
            resp.sendError(400, "Request syntax error!");
            System.err.println("missing targa");
            return;
        }

        if ((lat = req.getParameter("lat")) == null) {
            resp.sendError(400, "Request syntax error!");
            System.err.println("missing lat");
            return;
        }

        if ((lon = req.getParameter("lon")) == null) {
            resp.sendError(400, "Request syntax error!");
            System.err.println("missing long");
            return;
        }

        String query = "INSERT INTO Posizioni(veicolo, data, ora, latitudine, longitudine) " +
                "VALUES (?, DATE(NOW()), TIME(NOW()), ?, ?)";
        PreparedStatement s;
        try {
            s = con.prepareStatement(query);
            s.setString(1, targa);
            s.setDouble(2, Double.parseDouble(lat));
            s.setDouble(3, Double.parseDouble(lon));
            s.executeUpdate();
        } catch (SQLException | NumberFormatException e) {
            resp.sendError(500, e.getMessage());
            e.printStackTrace();
        }

    }

    public void destroy() {
    }
}