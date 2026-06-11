import packages.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;

public class Main extends Application {

    private GameLogic game;
    private GridPane gameGrid;
    private TextArea eventLogArea;
    private Label currentPlayerLabel;
    private Label marketPricesLabel;
    private Stage primaryStage;
    private Map<Player, VBox> playerResourcePanels;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }

    private void showMainMenu() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-padding: 40px; -fx-background-color: " + Constants.COLOR_BG + ";");

        Text title = new Text("Silicon Valley: The Tech Cartel");
        title.setStyle("-fx-font-size: 28px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");

        Button newGameBtn = new Button("New Game");
        newGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 200px;");
        newGameBtn.setOnAction(e -> showPlayerSetup());

        Button loadGameBtn = new Button("Load Game");
        loadGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 200px;");
        loadGameBtn.setOnAction(e -> loadSavedGame());

        Button exitBtn = new Button("Exit");
        exitBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 200px;");
        exitBtn.setOnAction(e -> Platform.exit());

        menuBox.getChildren().addAll(title, newGameBtn, loadGameBtn, exitBtn);

        Scene scene = new Scene(menuBox, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley: The Tech Cartel");
        primaryStage.show();
    }

    private void showPlayerSetup() {
        VBox setupBox = new VBox(10);
        setupBox.setAlignment(Pos.CENTER);
        setupBox.setStyle("-fx-padding: 20px; -fx-background-color: " + Constants.COLOR_BG + ";");

        Text title = new Text("Game Setup");
        title.setStyle("-fx-font-size: 24px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");

        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of Players:");
        countLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + ";");
        ComboBox<Integer> playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(2, 3, 4);
        playerCountCombo.setValue(2);
        playerCountBox.getChildren().addAll(countLabel, playerCountCombo);

        VBox nameBox = new VBox(5);
        Runnable updateNameFields = () -> {
            nameBox.getChildren().clear();
            int count = playerCountCombo.getValue();
            for (int i = 0; i < count; i++) {
                Label playerLabel = new Label("Player " + (i + 1) + ":");
                playerLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + ";");
                TextField tf = new TextField("Player " + (i + 1));
                nameBox.getChildren().addAll(playerLabel, tf);
            }
        };
        playerCountCombo.setOnAction(e -> updateNameFields.run());
        updateNameFields.run();

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> {
            List<String> names = new ArrayList<>();
            int playerCount = playerCountCombo.getValue();
            for (int i = 0; i < playerCount; i++) {
                TextField tf = (TextField) nameBox.getChildren().get(i * 2 + 1);
                names.add(tf.getText());
            }
            startGame(names);
        });

        setupBox.getChildren().addAll(title, playerCountBox, nameBox, startButton);

        Scene scene = new Scene(setupBox, 800, 600);
        primaryStage.setScene(scene);
    }

    private void startGame(List<String> playerNames) {
        game = new GameLogic(playerNames);
        game.startGame();
        showGameUI();
    }

    private void showGameUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + Constants.COLOR_BG + ";");

        // Menu bar
        MenuBar menuBar = new MenuBar();
        Menu gameMenu = new Menu("Game");
        MenuItem saveItem = new MenuItem("Save Game");
        saveItem.setOnAction(e -> saveGame());
        MenuItem loadItem = new MenuItem("Load Game");
        loadItem.setOnAction(e -> loadSavedGame());
        MenuItem exitItem = new MenuItem("Exit to Menu");
        exitItem.setOnAction(e -> showMainMenu());
        gameMenu.getItems().addAll(saveItem, loadItem, exitItem);

        Menu helpMenu = new Menu("Help");
        MenuItem rulesItem = new MenuItem("Game Rules");
        rulesItem.setOnAction(e -> showRules());
        helpMenu.getItems().add(rulesItem);

        menuBar.getMenus().addAll(gameMenu, helpMenu);
        root.setTop(menuBar);

        // Game grid
        gameGrid = new GridPane();
        gameGrid.setHgap(5);
        gameGrid.setVgap(5);
        gameGrid.setAlignment(Pos.CENTER);
        gameGrid.setStyle("-fx-padding: 20px;");
        updateGameGrid();

        // Info panel
        VBox infoPanel = new VBox(10);
        infoPanel.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a;");
        infoPanel.setPrefWidth(350);

        currentPlayerLabel = new Label("Current Player: " + game.getCurrentPlayer().getName());
        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");

        marketPricesLabel = new Label();
        updateMarketPrices();

        playerResourcePanels = new HashMap<>();
        VBox resourcesBox = new VBox(5);
        Label resourcesTitle = new Label("Player Resources:");
        resourcesTitle.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
        resourcesBox.getChildren().add(resourcesTitle);
        for (Player player : game.getPlayers()) {
            VBox playerBox = createPlayerResourcePanel(player);
            playerResourcePanels.put(player, playerBox);
            resourcesBox.getChildren().add(playerBox);
        }
        ScrollPane resourceScroll = new ScrollPane(resourcesBox);
        resourceScroll.setFitToWidth(true);
        resourceScroll.setPrefHeight(250);
        resourceScroll.setStyle("-fx-background: #1a1a1a;");

        eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setPrefHeight(200);
        eventLogArea.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: " + Constants.COLOR_TEXT + ";");

        // Action buttons
        Button rollButton = new Button("🎲 Roll Dice");
        rollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white;");
        rollButton.setOnAction(e -> rollDice());

        Button buildMVPButton = new Button("🏗️ Build MVP");
        buildMVPButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        buildMVPButton.setOnAction(e -> showBuildMVP());

        Button upgradeButton = new Button("🦄 Upgrade to Unicorn");
        upgradeButton.setStyle("-fx-font-size: 14px; -fx-background-color: #FF9800; -fx-text-fill: white;");
        upgradeButton.setOnAction(e -> showUpgradeMenu());

        Button buildPartnershipButton = new Button("🤝 Build Partnership");
        buildPartnershipButton.setStyle("-fx-font-size: 14px; -fx-background-color: #9C27B0; -fx-text-fill: white;");
        buildPartnershipButton.setOnAction(e -> showBuildPartnership());

        Button marketButton = new Button("💰 Market");
        marketButton.setStyle("-fx-font-size: 14px; -fx-background-color: #009688; -fx-text-fill: white;");
        marketButton.setOnAction(e -> showMarket());

        Button endTurnButton = new Button("⏭️ End Turn");
        endTurnButton.setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white;");
        endTurnButton.setOnAction(e -> endTurn());

        HBox buttonBox = new HBox(10, rollButton, buildMVPButton, upgradeButton, buildPartnershipButton, marketButton, endTurnButton);
        buttonBox.setAlignment(Pos.CENTER);

        infoPanel.getChildren().addAll(currentPlayerLabel, marketPricesLabel, resourceScroll, new Separator(), buttonBox, new Label("📋 Event Log:"), eventLogArea);

        ScrollPane gridScroll = new ScrollPane(gameGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setFitToHeight(true);
        root.setCenter(gridScroll);
        root.setRight(infoPanel);

        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley - " + game.getCurrentPlayer().getName() + "'s turn");
        updateEventLog();
        updateAllResources();
    }

    private VBox createPlayerResourcePanel(Player player) {
        VBox box = new VBox(3);
        box.setStyle("-fx-padding: 8px; -fx-border-color: #444; -fx-border-radius: 5px; -fx-background-color: #2a2a2a;");
        HBox header = new HBox(10);
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-fill: white; -fx-font-weight: bold;");
        Label scoreLabel = new Label("Score: " + player.calculateScore());
        scoreLabel.setStyle("-fx-fill: #FFD700;");
        header.getChildren().addAll(nameLabel, scoreLabel);
        FlowPane resourcesPane = new FlowPane(5, 3);
        box.getChildren().addAll(header, resourcesPane);
        box.setUserData(new Object[]{scoreLabel, resourcesPane});
        return box;
    }

    private void updatePlayerResources(Player player) {
        VBox box = playerResourcePanels.get(player);
        if (box == null) return;
        Object[] data = (Object[]) box.getUserData();
        Label scoreLabel = (Label) data[0];
        FlowPane resourcesPane = (FlowPane) data[1];
        scoreLabel.setText("Score: " + player.calculateScore());
        resourcesPane.getChildren().clear();
        Map<String, Integer> res = player.getResources().getAllResources();
        for (Map.Entry<String, Integer> e : res.entrySet()) {
            if (e.getValue() > 0) {
                Label l = new Label(e.getKey().substring(0, 3) + ":" + e.getValue());
                l.setStyle("-fx-background-color: " + getResourceColor(e.getKey()) + "; -fx-padding: 2px 5px; -fx-border-radius: 3px; -fx-text-fill: white;");
                resourcesPane.getChildren().add(l);
            }
        }
        int total = player.getResources().getTotalCards();
        Label totalLabel = new Label("Total: " + total);
        totalLabel.setStyle("-fx-text-fill: #FF9800; -fx-background-color: #333; -fx-padding: 2px 5px; -fx-border-radius: 3px;");
        resourcesPane.getChildren().add(totalLabel);
    }

    private String getResourceColor(String res) {
        switch (res) {
            case Constants.RESOURCE_TALENT: return "#443199";
            case Constants.RESOURCE_CAPITAL: return "#111844";
            case Constants.RESOURCE_CLOUD: return "#C13383";
            case Constants.RESOURCE_DATA: return "#E05454";
            case Constants.RESOURCE_PATENT: return "#792CA2";
            default: return "#555";
        }
    }

    private void updateAllResources() {
        for (Player p : game.getPlayers()) updatePlayerResources(p);
    }

    private void updateGameGrid() {
        gameGrid.getChildren().clear();
        Sector[][] map = game.getMap();
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                Sector s = map[i][j];
                StackPane square = new StackPane();
                square.setPrefSize(Constants.SECTOR_SIZE, Constants.SECTOR_SIZE);
                square.setStyle("-fx-border-color: white; -fx-border-width: 1px;");
                Rectangle bg = new Rectangle(Constants.SECTOR_SIZE, Constants.SECTOR_SIZE);
                bg.setFill(getColorFromType(s.getType()));
                bg.setArcWidth(10);
                bg.setArcHeight(10);
                if (s.hasAuditor()) {
                    Rectangle auditorBadge = new Rectangle(30, 30);
                    auditorBadge.setFill(Color.RED);
                    Text auditorText = new Text("🔍");
                    StackPane auditorPane = new StackPane(auditorBadge, auditorText);
                    StackPane.setAlignment(auditorPane, Pos.TOP_RIGHT);
                    square.getChildren().add(auditorPane);
                }
                Text typeText = new Text(s.getType());
                typeText.setStyle("-fx-font-size: 9px; -fx-fill: white;");
                Text diceText = new Text(s.getDiceNumber() > 0 ? String.valueOf(s.getDiceNumber()) : "⚖");
                diceText.setStyle("-fx-font-size: 20px; -fx-fill: white;");
                Node[] nodes = s.getNodes();
                for (int idx = 0; idx < 4; idx++) {
                    Node node = nodes[idx];
                    if (node != null && node.hasCompany()) {
                        Company c = node.getCompany();
                        String symbol = c instanceof Unicorn ? "🦄" : "★";
                        Text marker = new Text(symbol);
                        marker.setStyle("-fx-font-size: 20px; -fx-fill: gold;");
                        switch (idx) {
                            case 0: StackPane.setAlignment(marker, Pos.TOP_LEFT); break;
                            case 1: StackPane.setAlignment(marker, Pos.TOP_RIGHT); break;
                            case 2: StackPane.setAlignment(marker, Pos.BOTTOM_LEFT); break;
                            case 3: StackPane.setAlignment(marker, Pos.BOTTOM_RIGHT); break;
                        }
                        square.getChildren().add(marker);
                    }
                }
                VBox labels = new VBox(5, diceText, typeText);
                labels.setAlignment(Pos.CENTER);
                square.getChildren().addAll(bg, labels);
                gameGrid.add(square, j, i);
            }
        }
    }

    private Color getColorFromType(String type) {
        switch (type) {
            case Constants.SECTOR_TYPE_AI: return Color.web("#443199");
            case Constants.SECTOR_TYPE_FINTECH: return Color.web("#111844");
            case Constants.SECTOR_TYPE_CLOUD: return Color.web("#C13383");
            case Constants.SECTOR_TYPE_DATA: return Color.web("#E05454");
            case Constants.SECTOR_TYPE_PATENT: return Color.web("#792CA2");
            case Constants.SECTOR_TYPE_REGULATORY: return Color.web("#555");
            default: return Color.GRAY;
        }
    }

    private void updateMarketPrices() {
        StringBuilder sb = new StringBuilder("💰 Market:\n");
        for (Map.Entry<String, Integer> e : game.getMarket().getAllPrices().entrySet()) {
            sb.append("  ").append(e.getKey().substring(0, 3)).append(": ").append(e.getValue()).append("\n");
        }
        marketPricesLabel.setText(sb.toString());
        marketPricesLabel.setStyle("-fx-fill: white;");
    }

    private void updateEventLog() {
        StringBuilder sb = new StringBuilder();
        for (String ev : game.getEventLog()) sb.append("• ").append(ev).append("\n");
        eventLogArea.setText(sb.toString());
    }

    private void rollDice() {
        game.rollDice();
        updateGameGrid();
        updateMarketPrices();
        updateAllResources();
        updateEventLog();
        if (game.isGameFinished()) showWinner();
    }

    private void showBuildMVP() {
        List<Node> available = new ArrayList<>();
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                Node node = game.getNodes()[i][j];
                if (!node.hasCompany() && game.isValidPlacement(node)) available.add(node);
            }
        }
        if (available.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "No valid location for MVP!");
            a.showAndWait();
            return;
        }
        ChoiceDialog<Node> dialog = new ChoiceDialog<>(available.get(0), available);
        dialog.setTitle("Build MVP");
        dialog.setHeaderText("Select a node (cost: 1 Talent, 1 Capital, 1 Cloud, 1 Data)");
        dialog.showAndWait().ifPresent(node -> {
            try {
                game.buildMVP(game.getCurrentPlayer(), node);
                updateGameGrid();
                updateAllResources();
                updateEventLog();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private void showUpgradeMenu() {
        Player current = game.getCurrentPlayer();
        List<MVP> mvps = new ArrayList<>();
        for (Company c : current.getCompanies()) if (c instanceof MVP) mvps.add((MVP) c);
        if (mvps.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "No MVP to upgrade!");
            a.showAndWait();
            return;
        }
        ChoiceDialog<MVP> dialog = new ChoiceDialog<>(mvps.get(0), mvps);
        dialog.setTitle("Upgrade to Unicorn");
        dialog.setHeaderText("Upgrade MVP (cost: 2 Cloud, 3 Data)");
        dialog.showAndWait().ifPresent(mvp -> {
            try {
                game.upgradeToUnicorn(current, mvp);
                updateGameGrid();
                updateAllResources();
                updateEventLog();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private void showBuildPartnership() {
        Player current = game.getCurrentPlayer();
        List<Edge> validEdges = game.getValidPartnershipEdges(current);
        if (validEdges.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "No available edge for Partnership!");
            a.showAndWait();
            return;
        }
        ChoiceDialog<Edge> dialog = new ChoiceDialog<>(validEdges.get(0), validEdges);
        dialog.setTitle("Build Partnership");
        dialog.setHeaderText("Select an edge (cost: 1 Patent, 1 Capital)");
        dialog.showAndWait().ifPresent(edge -> {
            try {
                game.buildPartnership(current, edge);
                updateGameGrid();
                updateAllResources();
                updateEventLog();
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private void showMarket() {
        Stage marketStage = new Stage();
        VBox marketBox = new VBox(10);
        marketBox.setAlignment(Pos.CENTER);
        marketBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a;");
        Label title = new Label("Dynamic Market");
        title.setStyle("-fx-fill: white; -fx-font-size: 18px;");
        Player current = game.getCurrentPlayer();
        Label capital = new Label("Your Capital: " + current.getResources().getCount(Constants.RESOURCE_CAPITAL));
        capital.setStyle("-fx-fill: #FFD700;");
        for (String resource : Constants.RESOURCES) {
            if (resource.equals(Constants.RESOURCE_CAPITAL)) continue;
            HBox row = new HBox(10);
            int price = game.getMarket().getPrice(resource);
            int have = current.getResources().getCount(resource);
            Label info = new Label(resource + "  price:" + price + "  you have:" + have);
            info.setStyle("-fx-fill: white;");
            Button buy = new Button("Buy 1");
            buy.setOnAction(e -> {
                if (game.getMarket().buyResource(current, resource)) {
                    updateAllResources();
                    updateMarketPrices();
                    updateEventLog();
                    capital.setText("Your Capital: " + current.getResources().getCount(Constants.RESOURCE_CAPITAL));
                    marketStage.close();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Not enough Capital!");
                    a.showAndWait();
                }
            });
            Button sell = new Button("Sell 1");
            sell.setOnAction(e -> {
                if (have > 0) {
                    game.getMarket().sellResource(current, resource, 1);
                    updateAllResources();
                    updateMarketPrices();
                    updateEventLog();
                    capital.setText("Your Capital: " + current.getResources().getCount(Constants.RESOURCE_CAPITAL));
                    marketStage.close();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, "You don't have any " + resource);
                    a.showAndWait();
                }
            });
            row.getChildren().addAll(info, buy, sell);
            marketBox.getChildren().add(row);
        }
        marketBox.getChildren().addAll(title, capital);
        Scene scene = new Scene(marketBox, 400, 400);
        marketStage.setScene(scene);
        marketStage.show();
    }

    private void endTurn() {
        game.endTurn();
        if (!game.isGameFinished()) {
            currentPlayerLabel.setText("Current Player: " + game.getCurrentPlayer().getName());
            primaryStage.setTitle("Silicon Valley - " + game.getCurrentPlayer().getName() + "'s turn");
            updateEventLog();
            updateMarketPrices();
            updateAllResources();
        }
        if (game.isGameFinished()) showWinner();
    }

    private void saveGame() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save file", "*.sav"));
        File file = fc.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                game.saveGame(file.getAbsolutePath());
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Game saved!");
                a.showAndWait();
            } catch (IOException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Save failed: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }

    private void loadSavedGame() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Save file", "*.sav"));
        File file = fc.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                game = GameLogic.loadGame(file.getAbsolutePath());
                showGameUI();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Load failed: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }

    private void showRules() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Rules");
        a.setHeaderText("Silicon Valley: The Tech Cartel");
        a.setContentText("First to 10 points wins!\nMVP = 1pt, Unicorn = 2pt, longest Partnership chain = 2pt.\nRoll dice (2‑12). Sum = 7 → crisis.\nBuild MVP (1 Talent,1 Capital,1 Cloud,1 Data).\nUpgrade to Unicorn (2 Cloud,3 Data).\nBuild Partnership (1 Patent,1 Capital).");
        a.showAndWait();
    }

    private void showWinner() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Game Over");
        a.setHeaderText("🏆 " + game.getWinner().getName() + " wins! 🏆");
        a.showAndWait();
        showMainMenu();
    }

    public static void main(String[] args) {
        launch(args);
    }
}