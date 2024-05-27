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

    @FXML
    private Button fetchDataBtn, submitButton;

    @FXML
    private ComboBox<String> cityComboBox, favoritesComboBox;

    @FXML
    private ImageView conditionIcon, rainIcon2, windIcon, HumidityIcon, PressureIcon, background;

    @FXML
    private ToggleButton toggleButton;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField, confirmPasswordField;

    @FXML
    private Rectangle rectangle;

    @FXML
    private Label cityLable, errorLabel, discriptionLable, tempLable, feelLable, rainChanceLable, windSpeedLable, humidityLable, pressureLable, timeLabel, rain, cityLable11, cityLable111, cityLable1111;

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

        favoritesComboBox.setOnAction(event -> fetchFavoriteWeatherData());
    }

    private void setAuthenticationStatus() {
        Forma.setVisible(!isAuthenticated);
        Forma.setManaged(!isAuthenticated);
        Beck.setVisible(!isAuthenticated);
        Beck.setManaged(!isAuthenticated);
        if (isAuthenticated) {
            loadFavorites();
        }
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
                updateFavoriteIcon(city);
            } catch (IOException | InterruptedException | ParseException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void fetchFavoriteWeatherData() {
        String selectedCity = favoritesComboBox.getValue();
        if (selectedCity != null && !selectedCity.isEmpty()) {
            try {
                data = WeatherData.getData(selectedCity);
                setData(data);
                conditionIcon.setImage(data.getWeatherIcon());
                updateFavoriteIcon(selectedCity);
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
                    if (currentFavorites.contains(city)) {
                        currentFavorites = currentFavorites.replace(city + "&", "");
                    } else {
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
            loadFavorites(); // Reload favorites after update
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFavorites() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);

            String query = "SELECT favorites FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, usernameField.getText());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String favoritesString = resultSet.getString("favorites");
                if (favoritesString != null && !favoritesString.isEmpty()) {
                    List<String> favoriteCities = Arrays.asList(favoritesString.split("&"));
                    favorites.clear();
                    favorites.addAll(favoriteCities);
                    favoritesComboBox.setItems(FXCollections.observableArrayList(favoriteCities));
                } else {
                    favoritesComboBox.setItems(FXCollections.observableArrayList(new ArrayList<>()));
                }
            }

            connection.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateFavoriteIcon(String city) {
        ImageView starIcon = new ImageView(new Image(getClass().getResourceAsStream("../assets/star_filled.png")));
        starIcon.setFitWidth(20);
        starIcon.setFitHeight(20);
        if (favorites.contains(city)) {
            starIcon.setImage(new Image(getClass().getResourceAsStream("../assets/star_empty.png")));
        }
        Label favoriteLabel = new Label("", starIcon);
        favoriteLabel.setOnMouseClicked(event -> {
            updateFavorites(city);
            updateFavoriteIcon(city); // Update icon immediately after favorite status changes
        });
        cityLable.setGraphic(favoriteLabel);
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
        updateFavoriteIcon(data.getCity());
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
