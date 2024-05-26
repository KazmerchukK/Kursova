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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainController {

    private WeatherData data = new WeatherData();
    private String city;
    private Image image;
    private List<String> cities;
    private ObservableList<String> suggestions;

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

        // Удаляем предыдущий обработчик события выбора элемента
        cityComboBox.setOnAction(null);

        // Создаем список подсказок
        suggestions = FXCollections.observableArrayList(cities);

        // Добавляем обработчик для обновления подсказок при изменении текста
        cityComboBox.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                updateSuggestions(newValue);
                if (!cityComboBox.isShowing()) {
                    cityComboBox.show();
                }
            }
        });

        // Устанавливаем начальный список подсказок
        cityComboBox.setItems(suggestions);

        // Добавляем новый обработчик для события выбора элемента ComboBox
        cityComboBox.setOnAction(event -> fetchWeatherData());

        // Устанавливаем минимальную и максимальную высоту строки ввода
        cityComboBox.setMinHeight(24);
        cityComboBox.setMaxHeight(24);
    }

    private void updateSuggestions(String input) {
        // Очищаем список подсказок
        suggestions.clear();

        // Добавляем только те элементы, которые начинаются с введенного текста
        suggestions.addAll(cities.stream()
                .filter(city -> city.toLowerCase().startsWith(input.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList()));

        // Устанавливаем высоту выпадающего списка
        int itemCount = suggestions.size();
        double rowHeight = 24; // Высота строки в списке (можете изменить на свое значение)
        double maxHeight = 5 * rowHeight; // Максимальная высота списка
        double calculatedHeight = Math.min(itemCount * rowHeight, maxHeight);
        cityComboBox.setPrefHeight(calculatedHeight);

        // Обновляем выпадающий список, чтобы высота обновилась
        cityComboBox.hide();
        cityComboBox.show();
    }

    // Метод для обработки изменения текста в ComboBox
    private void handleCityInput(String input) {
        if (input.isEmpty()) {
            cityComboBox.hide();
        } else {
            updateSuggestions(input);
            if (!cityComboBox.isShowing()) {
                cityComboBox.show();
            }
        }
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