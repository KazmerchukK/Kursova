package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.json.simple.parser.ParseException;
import java.sql.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private Button fetchDataBtn;

    @FXML
    private ComboBox<String> cityComboBox;

    @FXML
    private ImageView conditionIcon;

    @FXML
    private Label cityLable, discriptionLable, tempLable, feelLable, rainChanceLable, windSpeedLable, humidityLable,
            pressureLable, timeLabel;

    @FXML
    public void initialize() {
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
        cityComboBox.setMaxHeight(24);
    }

    private void updateSuggestions(String input) {
        suggestions.clear();
        suggestions.addAll(cities.stream()
                .filter(city -> city.toLowerCase().startsWith(input.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList()));

        int itemCount = suggestions.size();
        double rowHeight = 24; // Высота строки в списке (можете изменить на свое значение)
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

        // Добавление звездочки для добавления в избранное
        ImageView starIcon = new ImageView(new Image(getClass().getResourceAsStream("../assets/star_filled.png")));
        starIcon.setFitWidth(20);
        starIcon.setFitHeight(20);
        Label favoriteLabel = new Label("", starIcon);
        favoriteLabel.setOnMouseClicked(event -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
                Statement statement = connection.createStatement();

                if (favorites.contains(data.getCity())) {
                    favorites.remove(data.getCity());
                    System.out.println("Removed from favorites: " + data.getCity());
                    starIcon.setImage(new Image(getClass().getResourceAsStream("../assets/star_empty.png")));
                    // Удаление из базы данных
                    String deleteQuery = "DELETE FROM favorites WHERE city = ?";
                    PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                    deleteStatement.setString(1, data.getCity());
                    deleteStatement.executeUpdate();
                } else {
                    favorites.add(data.getCity());
                    System.out.println("Added to favorites: " + data.getCity());
                    starIcon.setImage(new Image(getClass().getResourceAsStream("../assets/star_filled.png")));
                    // Добавление в базу данных
                    String insertQuery = "INSERT INTO favorites (city) VALUES (?)";
                    PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
                    insertStatement.setString(1, data.getCity());
                    insertStatement.executeUpdate();
                }

                connection.close(); // Закрываем соединение с базой данных
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        });
        cityLable.setGraphic(favoriteLabel);
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
}
