package me.mdbell.noexs.ui.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import me.mdbell.javafx.control.AddressSpinner;
import me.mdbell.javafx.control.HexSpinner;
import me.mdbell.noexs.ui.NoexesFiles;
import me.mdbell.noexs.ui.Settings;
import me.mdbell.noexs.ui.services.PointerSearchResult;
import me.mdbell.noexs.ui.services.PointerSearchService;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PointerSearchController implements IController {

    @FXML
    AddressSpinner addressSpinner;

    @FXML
    Spinner<Integer> depthSpinner;

    @FXML
    Spinner<Integer> threadsSpinner;

    @FXML
    HexSpinner offsetSpinner;

    @FXML
    Button dumpFileButton;

    @FXML
    TextField dumpFilePath;

    @FXML
    TextField resultText;

    @FXML
    ListView<PointerSearchResult> resultList;

    @FXML
    Button searchButton;

    @FXML
    Button cancelButton;

    @FXML
    AddressSpinner filterMaxAddress;
    @FXML
    AddressSpinner filterMinAddress;

    @FXML
    CheckBox filterCheckbox;

    @FXML
    AddressSpinner relativeAddress;

    private List<PointerSearchResult> unfilteredResults = new ArrayList<>();

    private ObservableList<PointerSearchResult> results;

    private MainController mc;

    private final PointerSearchService searchService = new PointerSearchService();

    @FXML
    public void initialize() {
        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10));

        threadsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Runtime.getRuntime().availableProcessors()));
        threadsSpinner.getValueFactory().setValue(Settings.getPointerThreadCount());

        depthSpinner.getValueFactory().setValue(Settings.getPointerDepth());
        offsetSpinner.getValueFactory().setValue(Settings.getPointerOffset());

        resultList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                resultText.setText(newValue.formatted(relativeAddress.getValue()));
            } else {
                resultText.setText("");
            }
        });

        resultList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PointerSearchResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.formatted(relativeAddress.getValue()));
                }
            }
        });

        dumpFilePath.textProperty().addListener((observable, oldValue, newValue) -> updateSearchButton());

        searchService.messageProperty().addListener((observable, oldValue, newValue) -> mc.setStatus(newValue));

        results = FXCollections.observableArrayList();
        resultList.setItems(results);

        relativeAddress.valueProperty().addListener((observable, oldValue, newValue) -> updateFilter());

        filterCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            filterMaxAddress.setDisable(!newValue);
            filterMinAddress.setDisable(!newValue);
            updateFilter();
        });

        filterMaxAddress.valueProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        filterMinAddress.valueProperty().addListener((observable, oldValue, newValue) -> updateFilter());
    }

    private void updateFilter() {
        results.clear();
        long min = filterMinAddress.getValue();
        long max = filterMaxAddress.getValue();
        if (filterCheckbox.isSelected()) {
            List<PointerSearchResult> filtered = new ArrayList<>();
            for (PointerSearchResult result : unfilteredResults) {
                long addr = result.getAddress();
                if (addr <= max && addr >= min) {
                    filtered.add(result);
                }
            }
            results.addAll(filtered);
        } else {
            results.addAll(unfilteredResults);
        }
    }

    private void updateSearchButton() {
        String dump = dumpFilePath.getText();
        searchButton.setDisable(dump.length() == 0);
    }

    @Override
    public void setMainController(MainController c) {
        this.mc = c;
    }

    public void onBrowseDumpFile(ActionEvent event) {
        mc.browseFile(false, dumpFilePath.textProperty(), "Please select a memory dump", "Memory Dump Files", "*.dmp");
    }

    public void onSearchAction(ActionEvent event) {
        searchService.setDumpPath(Paths.get(dumpFilePath.getText()));
        searchService.setMaxDepth(depthSpinner.getValue());
        searchService.setMaxOffset(offsetSpinner.getValue());
        searchService.setAddress(addressSpinner.getValue());
        searchService.setThreadCount(threadsSpinner.getValue());

        searchService.setOnFailed(event1 -> {
            mc.setStatus("Search Failed!");
            event1.getSource().getException().printStackTrace();
            toggleInput(false);
        });

        searchService.setOnSucceeded(event1 -> {
            Set<PointerSearchResult> results = (Set<PointerSearchResult>) event1.getSource().getValue();
            this.unfilteredResults.clear();
            this.unfilteredResults.addAll(results);
            mc.setStatus("Search Completed!");
            toggleInput(false);
            updateFilter();
        });

        mc.getProgressBar().progressProperty().bind(searchService.progressProperty());
        searchService.restart();

        toggleInput(true);
    }

    public void onCancelAction(ActionEvent event) {
        if (searchService.cancel()) {
            toggleInput(false);
        }
    }

    public void onExportListClicked(ActionEvent event) {
        System.out.println("clicked");
        int size = resultList.getItems().size();
        Long relAddress = relativeAddress.getValue();
        System.out.println("Size: " + size + "; relAddress: " + relAddress);
        if (resultList.getItems().size() > 0 && relativeAddress.getValue() != null) {
            System.out.println("we have items to export");
            List<PointerSearchResult> items = resultList
                    .getItems()
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());
            System.out.println("New size: " + items.size());
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("-- List of Addresses --").append("\n\n");
            for (PointerSearchResult item:
                 items) {
                String formatted = item.formatted(relativeAddress.getValue());
                strBuilder.append(formatted).append("\n");
                System.out.println(formatted);
            }
            try {
                File tmpFile = NoexesFiles.createTempFile("txt");
                if (!tmpFile.exists()) {
                    throw new IOException("tmp file doesn't exist");
                }
                RandomAccessFile randomAccessFile = new RandomAccessFile(tmpFile, "rw");
                randomAccessFile.seek(0);
                randomAccessFile.writeUTF(strBuilder.toString());
                randomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void stop() {
        Settings.setPointerOffset(offsetSpinner.getValue());
        Settings.setPointerDepth(depthSpinner.getValue());
        Settings.setPointerThreadCount(threadsSpinner.getValue());
    }

    private void toggleInput(boolean disabled) {
        addressSpinner.setDisable(disabled);
        depthSpinner.setDisable(disabled);
        threadsSpinner.setDisable(disabled);
        offsetSpinner.setDisable(disabled);
        dumpFileButton.setDisable(disabled);
        searchButton.setDisable(disabled);
        cancelButton.setDisable(!disabled);
    }

    public void setFilterMin(long address) {
        filterMinAddress.getValueFactory().setValue(address);
    }

    public void setFilterMax(long address) {
        filterMaxAddress.getValueFactory().setValue(address);
    }

    public void setRelativeAddress(long address) {
        relativeAddress.getValueFactory().setValue(address);
    }
}
