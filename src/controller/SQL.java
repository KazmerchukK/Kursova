package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQL {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/basemy";
    private static final String JDBC_USER = "admin";
    private static final String JDBC_PASSWORD = "admin";

    public static void main(String[] args) {
        try {
            // Загружаем драйвер
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Устанавливаем соединение
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            // Создаем объект Statement
            Statement statement = connection.createStatement();
            // Выполняем SQL запрос
            ResultSet resultSet = statement.executeQuery("SELECT * FROM pogoda");

            // Обрабатываем результаты запроса
            while (resultSet.next()) {
                System.out.println("ID: " + resultSet.getInt("id"));
                System.out.println("Name: " + resultSet.getString("name"));
            }


            // Закрываем ресурсы
            resultSet.close();
            statement.close();
            connection.close();
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}