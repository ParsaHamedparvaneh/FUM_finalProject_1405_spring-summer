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
    private VBox infoPanel;
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
        
        // Player count selection
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of Players:");
        countLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + ";");
        ComboBox<Integer> playerCountCombo = new ComboBox<>();
        playerCountCombo.getItems().addAll(2, 3, 4);
        playerCountCombo.setValue(2);
        playerCountBox.getChildren().addAll(countLabel, playerCountCombo);
        
        // Player name inputs
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
        
        // Role selection option
        CheckBox roleToggle = new CheckBox("Enable Founder Roles (costs 1 point)");
        roleToggle.setStyle("-fx-text-fill: " + Constants.COLOR_TEXT + ";");
        
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> {
            List<String> names = new ArrayList<>();
            int playerCount = playerCountCombo.getValue();
            for (int i = 0; i < playerCount; i++) {
                TextField tf = (TextField) nameBox.getChildren().get(i * 2 + 1);
                names.add(tf.getText());
            }
            startGame(names, roleToggle.isSelected());
        });
        
        setupBox.getChildren().addAll(title, playerCountBox, nameBox, roleToggle, startButton);
        
        Scene scene = new Scene(setupBox, 800, 600);
        primaryStage.setScene(scene);
    }
    
    private void startGame(List<String> playerNames, boolean withRoles) {
        game = new GameLogic(playerNames);
        game.startGame();
        
        if (withRoles) {
            showRoleSelection();
        } else {
            showGameUI();
        }
    }
    
    private void showRoleSelection() {
        Stage roleStage = new Stage();
        VBox roleBox = new VBox(10);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setStyle("-fx-padding: 20px; -fx-background-color: " + Constants.COLOR_BG + ";");
        
        Text title = new Text("Select Your Founder Roles");
        title.setStyle("-fx-font-size: 20px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
        
        List<Player> players = game.getPlayers();
        Map<Player, String> selectedRoles = new HashMap<>();
        
        // Roles available (each can only be taken once)
        List<String> availableRoles = new ArrayList<>(Arrays.asList("Trader", "CTO", "VC-Funded"));
        
        for (Player player : players) {
            VBox playerRoleBox = new VBox(5);
            playerRoleBox.setStyle("-fx-padding: 10px; -fx-border-color: #444; -fx-border-radius: 5px;");
            
            Label playerLabel = new Label(player.getName());
            playerLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.getItems().add("No Role");
            roleCombo.getItems().addAll(availableRoles);
            roleCombo.setValue("No Role");
            roleCombo.setStyle("-fx-font-size: 12px;");
            
            TextArea descArea = new TextArea();
            descArea.setEditable(false);
            descArea.setPrefHeight(60);
            descArea.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: " + Constants.COLOR_TEXT + ";");
            
            roleCombo.setOnAction(e -> {
                String selected = roleCombo.getValue();
                if (selected.equals("Trader")) {
                    descArea.setText("Trades with market at 3:1 rate instead of 4:1");
                } else if (selected.equals("CTO")) {
                    descArea.setText("Upgrades MVP to Unicorn using 1 less Cloud resource");
                } else if (selected.equals("VC-Funded")) {
                    descArea.setText("Starts with +2 Capital, holds 9 cards before tax (instead of 7)");
                } else {
                    descArea.setText("No special abilities, start with 0 points");
                }
            });
            
            playerRoleBox.getChildren().addAll(playerLabel, roleCombo, descArea);
            roleBox.getChildren().add(playerRoleBox);
            selectedRoles.put(player, null);
            
            final Player currentPlayer = player;
            roleCombo.setOnAction(e -> {
                String oldRole = selectedRoles.get(currentPlayer);
                if (oldRole != null && !oldRole.equals("No Role")) {
                    availableRoles.add(oldRole);
                }
                String newRole = roleCombo.getValue();
                if (!newRole.equals("No Role")) {
                    if (availableRoles.contains(newRole)) {
                        availableRoles.remove(newRole);
                        selectedRoles.put(currentPlayer, newRole);
                        // Update other comboboxes
                        updateRoleComboboxes(roleBox, players, selectedRoles, availableRoles);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Role Taken");
                        alert.setContentText("This role has already been selected by another player!");
                        roleCombo.setValue(oldRole != null ? oldRole : "No Role");
                    }
                } else {
                    selectedRoles.put(currentPlayer, null);
                }
            });
        }
        
        Button confirmBtn = new Button("Start Game");
        confirmBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        confirmBtn.setOnAction(e -> {
            for (Map.Entry<Player, String> entry : selectedRoles.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().equals("No Role")) {
                    game.assignRole(entry.getKey(), entry.getValue());
                }
            }
            roleStage.close();
            showGameUI();
        });
        
        roleBox.getChildren().add(confirmBtn);
        
        Scene scene = new Scene(roleBox, 500, 600);
        roleStage.setScene(scene);
        roleStage.setTitle("Select Roles");
        roleStage.show();
    }
    
    private void updateRoleComboboxes(VBox roleBox, List<Player> players, Map<Player, String> selectedRoles, List<String> availableRoles) {
        for (int i = 0; i < players.size(); i++) {
            VBox playerBox = (VBox) roleBox.getChildren().get(i);
            ComboBox<String> combo = (ComboBox<String>) playerBox.getChildren().get(1);
            String currentSelection = selectedRoles.get(players.get(i));
            
            combo.getItems().clear();
            combo.getItems().add("No Role");
            for (String role : availableRoles) {
                combo.getItems().add(role);
            }
            if (currentSelection != null) {
                combo.getItems().add(currentSelection);
                combo.setValue(currentSelection);
            } else {
                combo.setValue("No Role");
            }
        }
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
        infoPanel = new VBox(10);
        infoPanel.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a1a;");
        infoPanel.setPrefWidth(350);
        
        currentPlayerLabel = new Label("Current Player: " + game.getCurrentPlayer().getName());
        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
        
        marketPricesLabel = new Label();
        updateMarketPrices();
        
        // Player resources display
        playerResourcePanels = new HashMap<>();
        VBox resourcesBox = new VBox(5);
        Label resourcesTitle = new Label("Player Resources:");
        resourcesTitle.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        resourcesBox.getChildren().add(resourcesTitle);
        
        for (Player player : game.getPlayers()) {
            VBox playerBox = createPlayerResourcePanel(player);
            playerResourcePanels.put(player, playerBox);
            resourcesBox.getChildren().add(playerBox);
        }
        
        ScrollPane resourceScroll = new ScrollPane(resourcesBox);
        resourceScroll.setFitToWidth(true);
        resourceScroll.setPrefHeight(250);
        resourceScroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a;");
        
        eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setPrefHeight(200);
        eventLogArea.setStyle("-fx-control-inner-background: #2a2a2a; -fx-text-fill: " + Constants.COLOR_TEXT + ";");
        
        // Action buttons
        Button rollButton = new Button("🎲 Roll Dice");
        rollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 8px;");
        rollButton.setOnAction(e -> rollDice());
        
        Button buildButton = new Button("🏗️ Build Structure");
        buildButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 8px;");
        buildButton.setOnAction(e -> showBuildMenu());
        
        Button marketButton = new Button("💰 Trade with Market");
        marketButton.setStyle("-fx-font-size: 14px; -fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 8px;");
        marketButton.setOnAction(e -> showMarketMenu());
        
        Button endTurnButton = new Button("⏭️ End Turn");
        endTurnButton.setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 8px;");
        endTurnButton.setOnAction(e -> endTurn());
        
        HBox buttonBox = new HBox(10, rollButton, buildButton, marketButton, endTurnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        infoPanel.getChildren().addAll(currentPlayerLabel, marketPricesLabel, 
            resourceScroll, new Separator(), buttonBox, 
            new Label("📋 Event Log:"), eventLogArea);
        
        ScrollPane gridScroll = new ScrollPane(gameGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setFitToHeight(true);
        gridScroll.setStyle("-fx-background: " + Constants.COLOR_BG + ";");
        
        root.setCenter(gridScroll);
        root.setRight(infoPanel);
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley: The Tech Cartel - " + game.getCurrentPlayer().getName() + "'s Turn");
        
        updateEventLog();
        updateAllResources();
    }
    
    private VBox createPlayerResourcePanel(Player player) {
        VBox box = new VBox(3);
        box.setStyle("-fx-padding: 8px; -fx-border-color: #444; -fx-border-radius: 5px; -fx-background-color: #2a2a2a;");
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        
        Label scoreLabel = new Label("Score: " + player.calculateScore());
        scoreLabel.setStyle("-fx-fill: #FFD700; -fx-font-size: 12px;");
        
        headerBox.getChildren().addAll(nameLabel, scoreLabel);
        
        if (player.getRole() != null) {
            Label roleLabel = new Label("Role: " + player.getRole());
            roleLabel.setStyle("-fx-fill: #FF9800; -fx-font-size: 10px;");
            headerBox.getChildren().add(roleLabel);
        }
        
        FlowPane resourcesPane = new FlowPane(5, 3);
        resourcesPane.setStyle("-fx-padding: 3px;");
        
        box.getChildren().addAll(headerBox, resourcesPane);
        box.setUserData(new Object[]{scoreLabel, resourcesPane});
        
        return box;
    }
    
    private void updatePlayerResources(Player player) {
        VBox box = playerResourcePanels.get(player);
        if (box != null) {
            Object[] data = (Object[]) box.getUserData();
            Label scoreLabel = (Label) data[0];
            FlowPane resourcesPane = (FlowPane) data[1];
            
            scoreLabel.setText("Score: " + player.calculateScore());
            resourcesPane.getChildren().clear();
            
            Map<String, Integer> resources = player.getResources().getAllResources();
            for (Map.Entry<String, Integer> entry : resources.entrySet()) {
                if (entry.getValue() > 0) {
                    String shortName = entry.getKey().substring(0, Math.min(3, entry.getKey().length()));
                    Label resourceLabel = new Label(shortName + ": " + entry.getValue());
                    String color = getResourceColor(entry.getKey());
                    resourceLabel.setStyle("-fx-background-color: " + color + "; -fx-padding: 2px 5px; -fx-border-radius: 3px; -fx-text-fill: white; -fx-font-size: 10px;");
                    resourcesPane.getChildren().add(resourceLabel);
                }
            }
            
            int totalCards = player.getResources().getTotalCards();
            Label totalLabel = new Label("Total: " + totalCards);
            totalLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 10px; -fx-background-color: #333; -fx-padding: 2px 5px; -fx-border-radius: 3px;");
            resourcesPane.getChildren().add(totalLabel);
        }
    }
    
    private String getResourceColor(String resource) {
        switch (resource) {
            case Constants.RESOURCE_TALENT: return "#443199";
            case Constants.RESOURCE_CAPITAL: return "#111844";
            case Constants.RESOURCE_CLOUD: return "#C13383";
            case Constants.RESOURCE_DATA: return "#E05454";
            case Constants.RESOURCE_PATENT: return "#792CA2";
            default: return "#555555";
        }
    }
    
    private void updateAllResources() {
        for (Player player : game.getPlayers()) {
            updatePlayerResources(player);
        }
    }
    
    private void showBuildMenu() {
        Stage buildStage = new Stage();
        VBox buildBox = new VBox(10);
        buildBox.setAlignment(Pos.CENTER);
        buildBox.setStyle("-fx-padding: 20px; -fx-background-color: " + Constants.COLOR_BG + ";");
        
        Text title = new Text("Build Structure");
        title.setStyle("-fx-font-size: 18px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
        
        Label costLabel = new Label("Your Resources:");
        costLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + ";");
        
        Player current = game.getCurrentPlayer();
        StringBuilder resourcesStr = new StringBuilder();
        Map<String, Integer> resources = current.getResources().getAllResources();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            resourcesStr.append(entry.getKey()).append(": ").append(entry.getValue()).append("  ");
        }
        Label resourceDisplay = new Label(resourcesStr.toString());
        resourceDisplay.setStyle("-fx-fill: #FFD700; -fx-font-size: 12px;");
        
        Button mvpBtn = new Button("Build MVP");
        mvpBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        mvpBtn.setOnAction(e -> {
            showNodeSelectionForMVP();
            buildStage.close();
        });
        
        Label mvpCost = new Label("Cost: 1 Talent, 1 Capital, 1 Cloud, 1 Data");
        mvpCost.setStyle("-fx-fill: #aaa; -fx-font-size: 10px;");
        
        Button partnershipBtn = new Button("Build Partnership");
        partnershipBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        partnershipBtn.setOnAction(e -> {
            showEdgeSelectionForPartnership();
            buildStage.close();
        });
        
        Label partnershipCost = new Label("Cost: 1 Patent, 1 Capital");
        partnershipCost.setStyle("-fx-fill: #aaa; -fx-font-size: 10px;");
        
        Button upgradeBtn = new Button("Upgrade MVP to Unicorn");
        upgradeBtn.setStyle("-fx-font-size: 14px; -fx-background-color: #FF9800; -fx-text-fill: white;");
        upgradeBtn.setOnAction(e -> {
            showMVPSelectionForUpgrade();
            buildStage.close();
        });
        
        Label upgradeCost = new Label("Cost: 2 Cloud, 3 Data");
        upgradeCost.setStyle("-fx-fill: #aaa; -fx-font-size: 10px;");
        
        VBox mvpBox = new VBox(3, mvpBtn, mvpCost);
        VBox partnershipBox = new VBox(3, partnershipBtn, partnershipCost);
        VBox upgradeBox = new VBox(3, upgradeBtn, upgradeCost);
        
        mvpBox.setAlignment(Pos.CENTER);
        partnershipBox.setAlignment(Pos.CENTER);
        upgradeBox.setAlignment(Pos.CENTER);
        
        buildBox.getChildren().addAll(title, costLabel, resourceDisplay, 
            new Separator(), mvpBox, partnershipBox, upgradeBox);
        
        Scene scene = new Scene(buildBox, 400, 350);
        buildStage.setScene(scene);
        buildStage.setTitle("Build Menu");
        buildStage.show();
    }
    
    private void showNodeSelectionForMVP() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Build MVP");
        alert.setHeaderText("Select a Node to Build MVP");
        alert.setContentText("In the full implementation, you would click on a valid node on the map.\n\n" +
                            "Valid nodes must be at least 2 edges away from other companies.\n\n" +
                            "Cost: 1 Talent, 1 Capital, 1 Cloud, 1 Data");
        alert.showAndWait();
        
        // For demonstration, find first available node
        Player current = game.getCurrentPlayer();
        Node[][] nodes = game.getNodes();
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                Node node = nodes[i][j];
                if (!node.hasCompany() && game.isValidPlacement(node)) {
                    Map<String, Integer> cost = new HashMap<>();
                    cost.put(Constants.RESOURCE_TALENT, Constants.MVP_COST_TALENT);
                    cost.put(Constants.RESOURCE_CAPITAL, Constants.MVP_COST_CAPITAL);
                    cost.put(Constants.RESOURCE_CLOUD, Constants.MVP_COST_CLOUD);
                    cost.put(Constants.RESOURCE_DATA, Constants.MVP_COST_DATA);
                    
                    try {
                        game.buildMVP(current, node, cost);
                        updateGameGrid();
                        updateAllResources();
                        updateEventLog();
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Success");
                        success.setContentText("MVP built successfully!");
                        success.showAndWait();
                        return;
                    } catch (Exception ex) {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Cannot Build");
                        error.setContentText(ex.getMessage());
                        error.showAndWait();
                        return;
                    }
                }
            }
        }
        
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("No Valid Location");
        error.setContentText("No valid location found to build MVP!");
        error.showAndWait();
    }
    
    private void showEdgeSelectionForPartnership() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Build Partnership");
        alert.setHeaderText("Select an Edge to Build Partnership");
        alert.setContentText("In the full implementation, you would click on a valid edge on the map.\n\n" +
                            "Edges must be connected to your existing structures.\n\n" +
                            "Cost: 1 Patent, 1 Capital");
        alert.showAndWait();
    }
    
    private void showMVPSelectionForUpgrade() {
        Player current = game.getCurrentPlayer();
        List<MVP> mvps = new ArrayList<>();
        for (Company company : current.getCompanies()) {
            if (company instanceof MVP) {
                mvps.add((MVP) company);
            }
        }
        
        if (mvps.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No MVP");
            alert.setContentText("You don't have any MVP to upgrade!");
            alert.showAndWait();
            return;
        }
        
        List<String> mvpLocations = new ArrayList<>();
        for (MVP mvp : mvps) {
            mvpLocations.add("MVP at (" + mvp.getPosition().getPosition().x + ", " + mvp.getPosition().getPosition().y + ")");
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(mvpLocations.get(0), mvpLocations);
        dialog.setTitle("Upgrade to Unicorn");
        dialog.setHeaderText("Select MVP to upgrade");
        dialog.setContentText("Choose MVP (Cost: 2 Cloud, 3 Data):");
        
        dialog.showAndWait().ifPresent(location -> {
            int index = mvpLocations.indexOf(location);
            MVP selectedMVP = mvps.get(index);
            try {
                game.upgradeToUnicorn(current, selectedMVP);
                updateGameGrid();
                updateAllResources();
                updateEventLog();
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Success");
                success.setContentText("MVP upgraded to Unicorn successfully!");
                success.showAndWait();
            } catch (Exception ex) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Cannot Upgrade");
                error.setContentText(ex.getMessage());
                error.showAndWait();
            }
        });
    }
    
    private void showMarketMenu() {
        Stage marketStage = new Stage();
        VBox marketBox = new VBox(10);
        marketBox.setAlignment(Pos.CENTER);
        marketBox.setStyle("-fx-padding: 20px; -fx-background-color: " + Constants.COLOR_BG + ";");
        
        Text title = new Text("Dynamic Market");
        title.setStyle("-fx-font-size: 18px; -fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
        
        Player current = game.getCurrentPlayer();
        int capital = current.getResources().getCount(Constants.RESOURCE_CAPITAL);
        Label capitalLabel = new Label("Your Capital: " + capital + "💰");
        capitalLabel.setStyle("-fx-fill: #FFD700; -fx-font-size: 14px;");
        
        ScrollPane scrollPane = new ScrollPane();
        VBox marketItems = new VBox(5);
        marketItems.setAlignment(Pos.CENTER);
        
        for (String resource : Constants.RESOURCES) {
            if (!resource.equals(Constants.RESOURCE_CAPITAL)) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER);
                int price = game.getMarket().getPrice(resource);
                int count = current.getResources().getCount(resource);
                
                VBox infoBox = new VBox(2);
                Label resLabel = new Label(resource);
                resLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-weight: bold;");
                Label priceLabel = new Label("Price: " + price + "💰 | You have: " + count);
                priceLabel.setStyle("-fx-fill: #aaa; -fx-font-size: 11px;");
                infoBox.getChildren().addAll(resLabel, priceLabel);
                
                Button buyBtn = new Button("Buy 1");
                buyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 60px;");
                buyBtn.setOnAction(e -> {
                    if (game.getMarket().buyResource(current, resource)) {
                        marketStage.close();
                        updateAllResources();
                        updateMarketPrices();
                        updateEventLog();
                        eventLogArea.appendText(current.getName() + " bought 1 " + resource + "\n");
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("Not enough Capital! Need " + price + "💰");
                        alert.showAndWait();
                    }
                });
                
                Button sellBtn = new Button("Sell 1");
                sellBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 60px;");
                sellBtn.setOnAction(e -> {
                    if (count > 0) {
                        game.getMarket().sellResource(current, resource, 1);
                        marketStage.close();
                        updateAllResources();
                        updateMarketPrices();
                        updateEventLog();
                        eventLogArea.appendText(current.getName() + " sold 1 " + resource + "\n");
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setContentText("You don't have any " + resource + "!");
                        alert.showAndWait();
                    }
                });
                
                row.getChildren().addAll(infoBox, buyBtn, sellBtn);
                marketItems.getChildren().add(row);
            }
        }
        
        scrollPane.setContent(marketItems);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        scrollPane.setStyle("-fx-background: #1a1a1a;");
        
        marketBox.getChildren().addAll(title, capitalLabel, scrollPane);
        
        Scene scene = new Scene(marketBox, 500, 450);
        marketStage.setScene(scene);
        marketStage.setTitle("Market");
        marketStage.show();
    }
    
    private void updateGameGrid() {
        gameGrid.getChildren().clear();
        Sector[][] map = game.getMap();
        
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                Sector sector = map[i][j];
                
                StackPane square = new StackPane();
                square.setPrefSize(Constants.SECTOR_SIZE, Constants.SECTOR_SIZE);
                square.setStyle("-fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;");
                
                Color bgColor = getColorFromType(sector.getType());
                Rectangle bg = new Rectangle(Constants.SECTOR_SIZE, Constants.SECTOR_SIZE);
                bg.setFill(bgColor);
                bg.setArcWidth(10);
                bg.setArcHeight(10);
                
                // Add auditor indicator
                if (sector.hasAuditor()) {
                    Rectangle auditorBadge = new Rectangle(30, 30);
                    auditorBadge.setFill(Color.RED);
                    auditorBadge.setArcWidth(5);
                    auditorBadge.setArcHeight(5);
                    Text auditorText = new Text("🔍");
                    auditorText.setStyle("-fx-font-size: 16px;");
                    StackPane auditorPane = new StackPane(auditorBadge, auditorText);
                    StackPane.setAlignment(auditorPane, Pos.TOP_RIGHT);
                    square.getChildren().add(auditorPane);
                }
                
                Text typeText = new Text(sector.getType());
                typeText.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-fill: " + Constants.COLOR_TEXT + ";");
                
                Text diceText = new Text(sector.getDiceNumber() > 0 ? "" + sector.getDiceNumber() : "⚖");
                diceText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: " + Constants.COLOR_TEXT + ";");
                
                // Show companies on nodes
                Node[] nodes = sector.getNodes();
                for (int nodeIdx = 0; nodeIdx < nodes.length; nodeIdx++) {
                    Node node = nodes[nodeIdx];
                    if (node != null && node.hasCompany()) {
                        Company company = node.getCompany();
                        String symbol = company instanceof Unicorn ? "🦄" : "★";
                        Text companyMarker = new Text(symbol);
                        companyMarker.setStyle("-fx-font-size: 20px; -fx-fill: gold;");
                        
                        // Position based on which node
                        switch (nodeIdx) {
                            case 0:
                                StackPane.setAlignment(companyMarker, Pos.TOP_LEFT);
                                break;
                            case 1:
                                StackPane.setAlignment(companyMarker, Pos.TOP_RIGHT);
                                break;
                            case 2:
                                StackPane.setAlignment(companyMarker, Pos.BOTTOM_LEFT);
                                break;
                            case 3:
                                StackPane.setAlignment(companyMarker, Pos.BOTTOM_RIGHT);
                                break;
                        }
                        square.getChildren().add(companyMarker);
                    }
                }
                
                VBox labels = new VBox(5, diceText, typeText);
                labels.setStyle("-fx-alignment: center;");
                
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
            case Constants.SECTOR_TYPE_REGULATORY: return Color.web("#555555");
            default: return Color.GRAY;
        }
    }
    
    private void updateMarketPrices() {
        StringBuilder sb = new StringBuilder("💰 Market Prices:\n");
        Map<String, Integer> prices = game.getMarket().getAllPrices();
        for (Map.Entry<String, Integer> entry : prices.entrySet()) {
            sb.append("  ").append(entry.getKey().substring(0, Math.min(3, entry.getKey().length())))
              .append(": ").append(entry.getValue()).append("💰\n");
        }
        marketPricesLabel.setText(sb.toString());
        marketPricesLabel.setStyle("-fx-fill: " + Constants.COLOR_TEXT + "; -fx-font-size: 12px;");
    }
    
    private void updateEventLog() {
        StringBuilder sb = new StringBuilder();
        for (String event : game.getEventLog()) {
            sb.append("• ").append(event).append("\n");
        }
        eventLogArea.setText(sb.toString());
    }
    
    private void rollDice() {
        game.rollDice();
        updateGameGrid();
        updateMarketPrices();
        updateAllResources();
        updateEventLog();
        
        if (game.isGameFinished()) {
            showWinner();
        }
    }
    
    private void endTurn() {
        game.endTurn();
        currentPlayerLabel.setText("Current Player: " + game.getCurrentPlayer().getName());
        primaryStage.setTitle("Silicon Valley: The Tech Cartel - " + game.getCurrentPlayer().getName() + "'s Turn");
        updateEventLog();
        updateMarketPrices();
        updateAllResources();
        
        if (game.isGameFinished()) {
            showWinner();
        }
    }
    
    private void saveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Game");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showSaveDialog(primaryStage);
        
        if (file != null) {
            try {
                game.saveGame(file.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Save Successful");
                alert.setContentText("Game saved to " + file.getName());
                alert.showAndWait();
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Save Failed");
                alert.setContentText("Could not save game: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    private void loadSavedGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Game");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                game = GameLogic.loadGame(file.getAbsolutePath());
                showGameUI();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Failed");
                alert.setContentText("Could not load game: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    private void showRules() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Rules");
        alert.setHeaderText("📖 Silicon Valley: The Tech Cartel");
        alert.setContentText(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "🏆 WIN CONDITION: First to 10 points!\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "⭐ SCORING:\n" +
            "  • MVP: 1 point\n" +
            "  • Unicorn: 2 points\n" +
            "  • Longest Partnership chain: 2 points\n" +
            "  • Founder Role: -1 point\n\n" +
            "🎲 GAMEPLAY:\n" +
            "  • Roll dice each turn (2-12)\n" +
            "  • Sectors matching the dice number produce resources\n" +
            "  • Roll 7 = Regulatory Crisis (Taxes + Auditor)\n\n" +
            "💰 RESOURCES:\n" +
            "  • Talent, Capital, Cloud, Data, Patent\n\n" +
            "🏗️ BUILD COSTS:\n" +
            "  • MVP: 1 Talent, 1 Capital, 1 Cloud, 1 Data\n" +
            "  • Unicorn Upgrade: 2 Cloud, 3 Data\n" +
            "  • Partnership: 1 Patent, 1 Capital\n\n" +
            "🎭 FOUNDER ROLES:\n" +
            "  • Trader: Trade at 3:1 rate\n" +
            "  • CTO: Unicorn costs 1 less Cloud\n" +
            "  • VC-Funded: Start with +2 Capital, hold 9 cards\n\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        alert.showAndWait();
    }
    
    private void showWinner() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("WINNER!");
        alert.setContentText(game.getWinner().getName() + " won the game!\n\n" + "Final Score: " + game.getWinner().calculateScore() + " points");
        alert.showAndWait();
        showMainMenu();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}