package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/basemy";
    private static final String JDBC_USER = "admin";
    private static final String JDBC_PASSWORD = "admin";
    private WeatherData data = new WeatherData();
    private String city;
    private Image image;
    private List<String> cities;
    private ObservableList<String> suggestions;
    private Set<String> favorites = new HashSet<>();

/*    @FXML
    private Label tempLable;*/

    @FXML
    private Button fetchDataBtn, submitButton;

    @FXML
    private ComboBox<String> cityComboBox, favoritesСomboBox;

    @FXML
    private ImageView conditionIcon,rainIcon2 ,windIcon ,HumidityIcon ,PressureIcon, background;

    @FXML
    private ToggleButton toggleButton;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField, confirmPasswordField;

    @FXML
    private Rectangle rectangle;

    @FXML
    private Label cityLable,errorLabel, discriptionLable, tempLable, feelLable, rainChanceLable, windSpeedLable, humidityLable, pressureLable, timeLabel,rain, cityLable11, cityLable111, cityLable1111;

    @FXML
    private VBox Forma, Beck, Favorites;

    private boolean isRegistrationMode = false;
    private boolean isAuthenticated = false;

    @FXML
    public void initialize() {

        setAuthenticationStatus();

        updateTime();
        loadCities();
        cityComboBox.setEditable(true);

        cityComboBox.setOnAction(null);

        suggestions = FXCollections.observableArrayList(cities);
        cityComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                updateSuggestions(newValue);
                if (!cityComboBox.isShowing()) {
                    cityComboBox.show();
                }
            }
        });

        cityComboBox.setItems(suggestions);

        cityComboBox.setOnAction(event -> fetchWeatherData());

        cityComboBox.setMinHeight(24);
        cityComboBox.setMaxHeight(36);
        toggleButton.setOnAction(event -> {
            isRegistrationMode = !isRegistrationMode;
            if (isRegistrationMode) {
                toggleButton.setText("Switch to Authorization");
                confirmPasswordField.setVisible(true);
            } else {
                toggleButton.setText("Switch to Registration");
                confirmPasswordField.setVisible(false);
            }
        });

        submitButton.setOnAction(event -> {
            if (isRegistrationMode) {
                registerUser();
            } else {
                authenticateUser();
            }
        });
    }

    private void setAuthenticationStatus() {
        Forma.setVisible(!isAuthenticated);
        Forma.setManaged(!isAuthenticated);
        Beck.setVisible(!isAuthenticated);
        Beck.setManaged(!isAuthenticated);

    }
    private void updateSuggestions(String input) {
        suggestions.clear();
        suggestions.addAll(cities.stream()
                .filter(city -> city.toLowerCase().startsWith(input.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList()));

        int itemCount = suggestions.size();
        double rowHeight = 36; // Высота строки в списке (можете изменить на свое значение)
        double maxHeight = 5 * rowHeight; // Максимальная высота списка
        double calculatedHeight = Math.min(itemCount * rowHeight, maxHeight);
        cityComboBox.setPrefHeight(calculatedHeight);
        cityComboBox.hide();
        cityComboBox.show();
    }

    private void loadCities() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/assets/cities_ukraine.txt"), "UTF-8"))) {
            cities = reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void registerUser() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Statement statement = connection.createStatement();

            String checkUserQuery = "SELECT * FROM users WHERE username = ?";
            PreparedStatement checkUserStatement = connection.prepareStatement(checkUserQuery);
            checkUserStatement.setString(1, username);
            ResultSet resultSet = checkUserStatement.executeQuery();

            if (resultSet.next()) {
                errorLabel.setText("Username already exists");
                return;
            }

            String insertUserQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement insertUserStatement = connection.prepareStatement(insertUserQuery);
            insertUserStatement.setString(1, username);
            insertUserStatement.setString(2, password);
            insertUserStatement.executeUpdate();

            errorLabel.setText("Registration successful");
            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Error registering user");
        }
    }

    @FXML
    private void authenticateUser() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);

            String checkUserQuery = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement checkUserStatement = connection.prepareStatement(checkUserQuery);
            checkUserStatement.setString(1, username);
            checkUserStatement.setString(2, password);
            ResultSet resultSet = checkUserStatement.executeQuery();

            if (resultSet.next()) {
                isAuthenticated = true;
                setAuthenticationStatus();
                errorLabel.setText("Login successful");
                Forma.setVisible(false);
                Beck.setVisible(false);
            } else {
                errorLabel.setText("Invalid username or password");
            }

            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Error authenticating user");
        }
    }


    @FXML
    private void fetchData() {
        fetchWeatherData();
    }

    private void fetchWeatherData() {
        if (!cityComboBox.getEditor().getText().isEmpty()) {
            city = cityComboBox.getEditor().getText();
            try {
                data = WeatherData.getData(city);
                setData(data);
                conditionIcon.setImage(data.getWeatherIcon());
            } catch (IOException | InterruptedException | ParseException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateFavorites(String city) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Statement statement = connection.createStatement();

            String favoritesQuery = "SELECT favorites FROM users WHERE username = ?";
            PreparedStatement favoritesStatement = connection.prepareStatement(favoritesQuery);
            favoritesStatement.setString(1, usernameField.getText());
            ResultSet resultSet = favoritesStatement.executeQuery();

            if (resultSet.next()) {
                String currentFavorites = resultSet.getString("favorites");
                if (currentFavorites == null || currentFavorites.isEmpty()) {
                    currentFavorites = city + "&";
                } else {
                    // Проверяем, содержится ли уже этот город в избранных
                    if (currentFavorites.contains(city)) {
                        // Если содержится, удаляем его из списка избранных
                        currentFavorites = currentFavorites.replace(city + "&", "");
                    } else {
                        // Если не содержится, добавляем его в список избранных
                        currentFavorites += city + "&";
                    }
                }
                String updateFavoritesQuery = "UPDATE users SET favorites = ? WHERE username = ?";
                PreparedStatement updateFavoritesStatement = connection.prepareStatement(updateFavoritesQuery);
                updateFavoritesStatement.setString(1, currentFavorites);
                updateFavoritesStatement.setString(2, usernameField.getText());
                updateFavoritesStatement.executeUpdate();
            }

            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void setData(WeatherData data) {
        cityLable.setText(data.getCity());
        discriptionLable.setText(data.getDescription());
        tempLable.setText(data.getTemp() + "° C");
        feelLable.setText("feels like " + data.getFeelLike() + "° C");
        rainChanceLable.setText(data.getRain() + "%");
        windSpeedLable.setText(data.getWindSpeed() + " m/s");
        humidityLable.setText(data.getHumidity() + "%");
        pressureLable.setText(data.getPressure() + " hPa");

        ImageView starIcon = new ImageView(new Image(getClass().getResourceAsStream("../assets/star_filled.png")));
        starIcon.setFitWidth(20);
        starIcon.setFitHeight(20);
        Label favoriteLabel = new Label("", starIcon);
        favoriteLabel.setOnMouseClicked(event -> {
            updateFavorites(city);
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
                Statement statement = connection.createStatement();

                if (favorites.contains(data.getCity())) {
                    favorites.remove(data.getCity());
                    starIcon.setImage(new Image(getClass().getResourceAsStream("../assets/star_filled.png")));
                    String deleteQuery = "DELETE FROM favorites WHERE city = ?";
                    PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                    deleteStatement.setString(1, data.getCity());
                    deleteStatement.executeUpdate();

                } else {
                    favorites.add(data.getCity());
                    starIcon.setImage(new Image(getClass().getResourceAsStream("../assets/star_empty.png")));
                    String insertQuery = "INSERT INTO favorites (city) VALUES (?)";
                    PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                    insertStatement.setString(1, data.getCity());
                    insertStatement.executeUpdate();

                }

                connection.close();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        });
        cityLable.setGraphic(favoriteLabel);
        updateSearchHistory(data.getCity());
    }
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");

    @FXML
    private void updateTime() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                while (true) {
                    Platform.runLater(() -> timeLabel.setText(timeFormat.format(Calendar.getInstance().getTime())));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void updateSearchHistory(String city) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            Statement statement = connection.createStatement();

            String updateHistoryQuery = "UPDATE users SET history = CONCAT(history, ?) WHERE username = ?";
            PreparedStatement updateHistoryStatement = connection.prepareStatement(updateHistoryQuery);
            updateHistoryStatement.setString(1, city + "&");
            updateHistoryStatement.setString(2, usernameField.getText());
            updateHistoryStatement.executeUpdate();

            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
}
