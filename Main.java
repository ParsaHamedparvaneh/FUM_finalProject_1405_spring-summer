import javafx.scene.*;
import javafx.util.*;
import javafx.animation.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

class HELPERS
{
    public static double getVertexDistance(int v1Idx, int v2Idx)
    {
        int cols = CONSTS.MAX_WIDTH + 1;
        int row1 = v1Idx /cols;
        int col1 = v1Idx % cols;
        int row2 = v2Idx / cols;
        int col2 = v2Idx % cols;
        
        double dx = row1-row2;
        double dy = col1-col2;
        return Math.sqrt(dx*dx + dy*dy);
    }

    public static boolean isAdjacent(int v1Idx, int v2Idx) { return getVertexDistance(v1Idx, v2Idx) <= 1; }
    public static int getCardCount(Player p)
    {
        int sum = 0;
        sum += p.talentCount;
        sum += p.capitalCount;
        sum += p.cloudCount;
        sum += p.patentCount;
        sum += p.dataCount;
        return sum;
    }
    public static int[] getVertexIdxFromSectorIdx(int sectorIdx)
    {
        int sectorsPerRow = CONSTS.MAX_WIDTH;
        int verticesPerRow = CONSTS.MAX_WIDTH + 1;
        
        int sectorRow = sectorIdx/sectorsPerRow;
        int sectorCol = sectorIdx % sectorsPerRow;
        
        int topLeftVertex = sectorRow * verticesPerRow + sectorCol;
        
        int fin[] = new int[4];
        fin[0] = topLeftVertex;
        fin[1] = topLeftVertex + 1;
        fin[2] = topLeftVertex + verticesPerRow;
        fin[3] = topLeftVertex + verticesPerRow + 1;
        return fin;
    }

    public static void drawSectors(Pane root, Sector SECTORS[])
    {
        for (int i = 0; i<SECTORS.length; i++)
        {
            Sector sector = SECTORS[i];
            int row = i/CONSTS.MAX_HEIGHT;
            int col = i%CONSTS.MAX_WIDTH;
            
            double x = CONSTS.OFFSET_X + col * CONSTS.CELL_SIZE + (CONSTS.CELL_SIZE - CONSTS.SECTOR_SIZE) / 2;
            double y = CONSTS.OFFSET_Y + row * CONSTS.CELL_SIZE + (CONSTS.CELL_SIZE - CONSTS.SECTOR_SIZE) / 2;
            
            StackPane sectorPane = new StackPane();
            sectorPane.setLayoutX(x);
            sectorPane.setLayoutY(y);
            sectorPane.setPrefSize(CONSTS.SECTOR_SIZE, CONSTS.SECTOR_SIZE);
            
            final Sector currentSector = sector;
            sectorPane.setOnMouseClicked(event -> {
                if (Main.isSelectingSector)
                    Main.selectSector(currentSector);
            });
            sectorPane.setCursor(javafx.scene.Cursor.HAND);
            
            Rectangle rect = new Rectangle(CONSTS.SECTOR_SIZE, CONSTS.SECTOR_SIZE);
            rect.setFill(Color.web(CONSTS.SECTOR_COLORS_HM.get(sector.type)));
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(2);

            if (sector.hasTaxAgent)
                sectorPane.setOpacity(0.4);

            if (sector.hasTaxAgent)
            {
                Color originalColor = Color.web(CONSTS.SECTOR_COLORS_HM.get(sector.type));
                rect.setFill(originalColor.desaturate().deriveColor(0, 0, 0.5, 0.5));
            }
            else
            {
                rect.setFill(Color.web(CONSTS.SECTOR_COLORS_HM.get(sector.type)));
                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(2);
            }
            
            VBox vbox = new javafx.scene.layout.VBox(5);
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            
            if (sector.hasTaxAgent)
            {
                Text warningText = new Text("X");
                warningText.setFont(Font.font(20));
                warningText.setFill(Color.LIGHTGRAY);
                vbox.getChildren().add(warningText);
            }

            Text typeText = new Text(sector.type);
            typeText.setFill(Color.WHITE);
            typeText.setFont(CONSTS.CUSTOM_FONT);
            typeText.setStyle("-fx-font-weight: bold;");
            
            Text dieText = new Text(String.valueOf(sector.dieNum));
            dieText.setFill(Color.WHITE);
            dieText.setFont(CONSTS.CUSTOM_FONT);
            dieText.setStyle("-fx-font-weight: bold;");

            if (sector.hasTaxAgent)
            {
                typeText.setFill(Color.LIGHTGRAY);
                dieText.setFill(Color.LIGHTGRAY);
            }
            
            vbox.getChildren().addAll(typeText, dieText);
            sectorPane.getChildren().addAll(rect, vbox);
            root.getChildren().add(sectorPane);
        }
    }

    public static void drawVertices(Pane root, Vertex VERTICES[][])
    {
        for (int i = 0; i<CONSTS.MAX_HEIGHT+1; i++)
        {
            for (int j = 0; j<CONSTS.MAX_WIDTH+1; j++)
            {
                Vertex vertex = VERTICES[i][j];
                double x = CONSTS.OFFSET_X + j * CONSTS.CELL_SIZE;
                double y = CONSTS.OFFSET_Y + i * CONSTS.CELL_SIZE;
                
                StackPane stackPane = new StackPane();
                stackPane.setLayoutX(x - CONSTS.VERTEX_RADIUS);
                stackPane.setLayoutY(y - CONSTS.VERTEX_RADIUS);
                stackPane.setPrefSize(CONSTS.VERTEX_RADIUS * 2, CONSTS.VERTEX_RADIUS * 2);

                stackPane.setOnMouseEntered(event -> {
                    if (Main.canInteractWithVertices && Main.isVertexInteractable(vertex))
                    {
                        Timeline timeline = HELPERS.createCustomScaleAnimation(stackPane, 1.4, 800);
                        timeline.play();
                    }
                });
                stackPane.setOnMouseExited(event -> {
                    Timeline timeline = HELPERS.createCustomScaleAnimation(stackPane, 1.0, 800);
                    timeline.play();
                });
                stackPane.setOnMouseClicked(event -> {
                    if (Main.canInteractWithVertices && !Main.isPayingTax && Main.isVertexInteractable(vertex))
                    {
                        Player currentPlayer = Main.getCurrentPlayer();
                        
                        if (Main.isInitializationNobat)
                        {
                            List<Vertex> selected = Main.initSelectedVertices.get(currentPlayer);
                            if (vertex.owner == null && (selected == null || selected.size() < 2))
                            {
                                Main.pendingInitVertex = vertex;
                                Main.diceStatusText.setText("⚄: INITIALIZATION - PLAYER " + (currentPlayer.idx+1) + " NOW SELECT AN EDGE");
                            }
                        }
                        else if (vertex.owner == currentPlayer || vertex.owner == null)
                        {
                            if (vertex.type != CONSTS.VERTEX_TYPE_UNICORN)
                                showPurchaseMenu(root, vertex, currentPlayer);
                            else
                                showVertexInfo(root, vertex, currentPlayer);
                        }
                    }
                });
                stackPane.setCursor(javafx.scene.Cursor.HAND);
                
                Circle circle = new Circle(CONSTS.VERTEX_RADIUS, CONSTS.VERTEX_RADIUS, CONSTS.VERTEX_RADIUS);
                circle.setFill(getVertexColor(vertex));
                if (vertex.type.equals(CONSTS.VERTEX_TYPE_UNICORN))
                {
                    DropShadow ds = new DropShadow();
                    ds.setRadius(CONSTS.VERTEX_RADIUS);
                    ds.setSpread(0.4);
                    ds.setOffsetX(0);
                    ds.setOffsetY(0);
                    ds.setColor(Color.web(CONSTS.COLOR_GOLD));
                    circle.setEffect(ds);
                }

                Text idxText = new Text(String.valueOf(vertex.idx));
                idxText.setFont(CONSTS.CUSTOM_FONT);
                idxText.setStyle("-fx-font-weight: bold;");
                idxText.setFill(Color.BLACK);
                
                stackPane.getChildren().addAll(circle, idxText);
                root.getChildren().add(stackPane);
            }
        }
    }
    public static void drawEdges(Pane root, Edge horizontalEdges[][], Edge verticalEdges[][])
    {
        for (int i = 0; i<horizontalEdges.length; i++)
        {
            for (int j = 0; j<horizontalEdges[i].length; j++)
            {
                Edge edge = horizontalEdges[i][j];
                double x1 = CONSTS.OFFSET_X + j * CONSTS.CELL_SIZE;
                double y = CONSTS.OFFSET_Y + i * CONSTS.CELL_SIZE;
                double x2 = x1 + CONSTS.CELL_SIZE;
                
                Line line = new Line(x1, y, x2, y);
                line.setStroke(getEdgeColor(edge));
                line.setStrokeWidth(4);
                line.setUserData(edge);
                
                line.setOnMouseEntered(event -> {
                    if (Main.canInteractWithVertices && (edge.owner == null || edge.owner == Main.getCurrentPlayer()))
                        line.setStroke(Color.LIGHTGRAY);
                });
                
                line.setOnMouseExited(event -> {
                    if (Main.canInteractWithVertices && (edge.owner == null || edge.owner == Main.getCurrentPlayer()))
                    {
                        line.setStrokeWidth(4);
                        line.setStroke(getEdgeColor(edge));
                    }
                });
                
                line.setOnMouseClicked(event -> {
                    if (Main.canInteractWithVertices && edge.owner == null)
                    {
                        Player currentPlayer = Main.getCurrentPlayer();
                        if (Main.isInitializationNobat)
                        {
                            if (Main.pendingInitVertex != null && Main.canBuyEdge(edge, currentPlayer))
                            {
                                Main.handleInitializationNobat(Main.pendingInitVertex, edge, currentPlayer);
                                Main.pendingInitVertex = null;
                            }
                        }
                        else if (Main.canBuyEdge(edge, currentPlayer))
                            Main.showEdgePurchaseDialog(root, edge, currentPlayer);
                    }
                });
                line.setCursor(javafx.scene.Cursor.HAND);
                
                root.getChildren().add(line);
            }
        }
        
        for (int i = 0; i<verticalEdges.length; i++)
        {
            for (int j = 0; j<verticalEdges[i].length; j++)
            {
                Edge edge = verticalEdges[i][j];
                double x = CONSTS.OFFSET_X + j * CONSTS.CELL_SIZE;
                double y1 = CONSTS.OFFSET_Y + i * CONSTS.CELL_SIZE;
                double y2 = y1 + CONSTS.CELL_SIZE;
                
                javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y1, x, y2);
                line.setStroke(getEdgeColor(edge));
                line.setStrokeWidth(4);
                line.setUserData(edge);
                
                line.setOnMouseEntered(event -> {
                    if (Main.canInteractWithVertices && (edge.owner == null || edge.owner == Main.getCurrentPlayer()))
                        line.setStroke(Color.LIGHTGRAY);
                });
                
                line.setOnMouseExited(event -> {
                    if (Main.canInteractWithVertices && (edge.owner == null || edge.owner == Main.getCurrentPlayer()))
                    {
                        line.setStrokeWidth(4);
                        line.setStroke(getEdgeColor(edge));
                    }
                });
                
                line.setOnMouseClicked(event -> {
                    if (Main.canInteractWithVertices && edge.owner == null)
                    {
                        Player currentPlayer = Main.getCurrentPlayer();
                        if (Main.canBuyEdge(edge, currentPlayer))
                            Main.showEdgePurchaseDialog(root, edge, currentPlayer);
                    }
                });
                line.setCursor(javafx.scene.Cursor.HAND);
                
                root.getChildren().add(line);
            }
        }
    }

    public static void showPurchaseMenu(Pane root, Vertex vertex, Player player)
    {
        if (Main.isInitializationNobat)
        {
            Main.pendingInitVertex = vertex;
            Main.diceStatusText.setText("⚄: INITIALIZATION - PLAYER " + (Main.currentPlayerIdx + 1) + " NOW SELECT AN EDGE");
            return;
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = root.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox menu = new VBox(15);
        menu.setAlignment(javafx.geometry.Pos.CENTER);
        menu.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        menu.setPrefWidth(300);
        menu.setPrefHeight(280);
        
        Text title = new Text("VERTEX " + vertex.idx);
        title.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 18));
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");
        
        Button mvpBtn = new Button("TURN TO MVP");
        mvpBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_GREEN+"; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5;");
        mvpBtn.setCursor(javafx.scene.Cursor.HAND);
        mvpBtn.setOnMouseEntered(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(mvpBtn, 1.2, 800);
            timeline.play();
        });
        mvpBtn.setOnMouseExited(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(mvpBtn, 1.0, 800);
            timeline.play();
        });
        mvpBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
            showPurchaseDialog(root, vertex, "MVP", player);
        });
        
        Button unicornBtn = new Button("TURN TO UNICORN");
        unicornBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_GREEN+"; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10; -fx-background-radius: 5;");
        unicornBtn.setCursor(javafx.scene.Cursor.HAND);
        unicornBtn.setOnMouseEntered(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(unicornBtn, 1.2, 800);
            timeline.play();
        });
        unicornBtn.setOnMouseExited(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(unicornBtn, 1.0, 800);
            timeline.play();
        });
        unicornBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
            showPurchaseDialog(root, vertex, "UNICORN", player);
        });
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.setStyle("-fx-background-color: #666; -fx-text-fill: white; -fx-padding: 8; -fx-font-weight: bold; -fx-background-radius: 5;");
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        cancelBtn.setOnMouseEntered(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(cancelBtn, 1.2, 800);
            timeline.play();
        });
        cancelBtn.setOnMouseExited(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(cancelBtn, 1.0, 800);
            timeline.play();
        });
        cancelBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
        });
        
        menu.getChildren().clear();
        menu.getChildren().add(title);
        if (vertex.type.equals(CONSTS.VERTEX_TYPE_NONE))
            menu.getChildren().add(mvpBtn);
        else if (vertex.type.equals(CONSTS.VERTEX_TYPE_MVP))
            menu.getChildren().add(unicornBtn);
        menu.getChildren().add(cancelBtn);
        menu.setLayoutX((CONSTS.WINDOW_WIDTH - menu.getPrefWidth()) / 2);
        menu.setLayoutY((CONSTS.WINDOW_HEIGHT - menu.getPrefHeight()) / 2);
        
        overlay.getChildren().add(menu);
        root.getChildren().add(overlay);
    }

    public static void showVertexInfo(Pane root, Vertex vertex, Player player)
    {
        System.out.println("UNICORN " + vertex.idx + " owned by Player " + (vertex.owner.idx + 1));
    }

    public static Color getVertexColor(Vertex vertex)
    {
        if (vertex.owner == null) return Color.LIGHTGRAY;
        
        int playerIdx = vertex.owner.idx;
        if (playerIdx == 0) return Color.web(CONSTS.PLAYER_1_COLOR);
        if (playerIdx == 1) return Color.web(CONSTS.PLAYER_2_COLOR);
        if (playerIdx == 2) return Color.web(CONSTS.PLAYER_3_COLOR);
        if (playerIdx == 3) return Color.web(CONSTS.PLAYER_4_COLOR);
        
        return Color.LIGHTGRAY;
    }
    public static Color getEdgeColor(Edge edge)
    {
        if (edge.owner == null) return Color.LIGHTGRAY;
        
        int playerIdx = edge.owner.idx;
        if (playerIdx == 0) return Color.web(CONSTS.PLAYER_1_COLOR);
        if (playerIdx == 1) return Color.web(CONSTS.PLAYER_2_COLOR);
        if (playerIdx == 2) return Color.web(CONSTS.PLAYER_3_COLOR);
        if (playerIdx == 3) return Color.web(CONSTS.PLAYER_4_COLOR);
        
        return Color.LIGHTGRAY;
    }

    public static Timeline createCustomScaleAnimation(Node node, double targetScale, double durationMs)
    {
        Timeline timeline = new Timeline();
        double durationPerStep = durationMs / 1000.0 / (CONSTS.PRANS.length - 1);
        
        double startScale = node.getScaleX();
        
        for (int i = 0; i<CONSTS.PRANS.length; i++)
        {
            double scale = startScale + (targetScale - startScale) * CONSTS.PRANS[i];
            timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(i * durationPerStep),
                    new KeyValue(node.scaleXProperty(), scale),
                    new KeyValue(node.scaleYProperty(), scale)
                )
            );
        }
        return timeline;
    }

    public static void showPurchaseDialog(Pane root, Vertex vertex, String type, Player player)
    {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = root.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(300);
        dialog.setPrefHeight(250);
        
        Text titleText = new Text("GET " + type);
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        titleText.setFill(Color.WHITE);
        titleText.setStyle("-fx-font-weight: bold;");
        
        VBox costBox = new VBox(5);
        costBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text costTitle = new Text("COSTS:");
        costTitle.setFill(Color.WHITE);
        costTitle.setFont(Font.font(14));
        
        Map<String, Integer> costs = null;
        if (type.equals("MVP"))
            costs = CONSTS.MVP_COST.get(0);
        else if (type.equals("UNICORN"))
            costs = CONSTS.UNICORN_COST.get(0);

        if (type.equals("UNICORN") && player.role == CONSTS.PLAYER_ROLE_NERD_AHH)
            costs.put(CONSTS.REWARD_TYPE_CLOUD, 1);

        VBox resourceList = new VBox(3);
        resourceList.setAlignment(javafx.geometry.Pos.CENTER);
        
        if (costs != null) {
            for (Map.Entry<String, Integer> entry : costs.entrySet())
            {
                String resourceName = entry.getKey();
                int required = entry.getValue();
                int has = 0;
                
                switch (resourceName)
                {
                    case CONSTS.REWARD_TYPE_AI: has = player.talentCount; break;
                    case CONSTS.REWARD_TYPE_FINTECH: has = player.capitalCount; break;
                    case CONSTS.REWARD_TYPE_CLOUD: has = player.cloudCount; break;
                    case CONSTS.REWARD_TYPE_IP: has = player.patentCount; break;
                    case CONSTS.REWARD_TYPE_DATA: has = player.dataCount; break;
                }
                
                Text resourceText = new Text(resourceName + ": " + has + "/" + required);
                resourceText.setFill(has >= required ? Color.web(CONSTS.COLOR_GREEN) : Color.web(CONSTS.COLOR_RED));
                resourceText.setFont(Font.font(12));
                resourceList.getChildren().add(resourceText);
            }
        }
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button purchaseBtn = new Button("PAY");
        purchaseBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_GREEN+"; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        purchaseBtn.setCursor(javafx.scene.Cursor.HAND);
        purchaseBtn.setOnMouseEntered(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(purchaseBtn, 1.2, 800);
            timeline.play();
        });
        purchaseBtn.setOnMouseExited(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(purchaseBtn, 1.0, 800);
            timeline.play();
        });
        purchaseBtn.setOnAction(e -> {
            boolean success = processPurchase(vertex, type, player);
            if (success)
            {
                root.getChildren().remove(overlay);
                refreshVertexColor(root, vertex);
            }
            else
            {
                Text errorMsg = new Text("GET YO BROKE AHH OUTTA HERE");
                errorMsg.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(errorMsg);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(errorMsg));
                pause.play();
            }
        });
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_RED+"; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        cancelBtn.setOnMouseEntered(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(cancelBtn, 1.2, 800);
            timeline.play();
        });
        cancelBtn.setOnMouseExited(e -> {
            Timeline timeline = HELPERS.createCustomScaleAnimation(cancelBtn, 1.0, 800);
            timeline.play();
        });
        cancelBtn.setOnAction(e -> {
            root.getChildren().remove(overlay);
        });
        
        buttonBox.getChildren().addAll(purchaseBtn, cancelBtn);
        costBox.getChildren().addAll(costTitle, resourceList);
        dialog.getChildren().addAll(titleText, costBox, buttonBox);
        
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
    }

    public static boolean processPurchase(Vertex vertex, String type, Player player)
    {
        Map<String, Integer> costs = null;
        
        if (type.equals("MVP"))
            costs = CONSTS.MVP_COST.get(0);
        else if (type.equals("UNICORN"))
            costs = CONSTS.UNICORN_COST.get(0);
        
        boolean hasEnough = true;
        for (Map.Entry<String, Integer> entry : costs.entrySet())
        {
            String resource = entry.getKey();
            int required = entry.getValue();
            int has = 0;
            
            switch (resource)
            {
                case CONSTS.REWARD_TYPE_AI: has = player.talentCount; break;
                case CONSTS.REWARD_TYPE_FINTECH: has = player.capitalCount; break;
                case CONSTS.REWARD_TYPE_CLOUD: has = player.cloudCount; break;
                case CONSTS.REWARD_TYPE_IP: has = player.patentCount; break;
                case CONSTS.REWARD_TYPE_DATA: has = player.dataCount; break;
            }
            
            if (has < required)
            {
                hasEnough = false;
                break;
            }
        }
        
        if (!hasEnough) return false;
        
        for (Map.Entry<String, Integer> entry : costs.entrySet())
        {
            String resource = entry.getKey();
            int required = entry.getValue();
            
            switch (resource)
            {
                case CONSTS.REWARD_TYPE_AI: player.talentCount -= required; break;
                case CONSTS.REWARD_TYPE_FINTECH: player.capitalCount -= required; break;
                case CONSTS.REWARD_TYPE_CLOUD: player.cloudCount -= required; break;
                case CONSTS.REWARD_TYPE_IP: player.patentCount -= required; break;
                case CONSTS.REWARD_TYPE_DATA: player.dataCount -= required; break;
            }
        }
        
        vertex.type = type;
        vertex.owner = player;
        
        if (type.equals("MVP"))
            player.score += 1;
        else if (type.equals("UNICORN"))
            player.score += 1;
        
        Main.updatePlayerInfo();
        return true;
    }

    public static void refreshVertexColor(Pane root, Vertex targetVertex)
    {
        int vertexIdx = targetVertex.idx;
        int row = vertexIdx / (CONSTS.MAX_WIDTH + 1);
        int col = vertexIdx % (CONSTS.MAX_WIDTH + 1);
        
        for (Node node : root.getChildren())
        {
            if (node instanceof StackPane)
            {
                StackPane sp = (StackPane) node;
                double x = sp.getLayoutX() + CONSTS.VERTEX_RADIUS;
                double y = sp.getLayoutY() + CONSTS.VERTEX_RADIUS;
                
                int expectedX = CONSTS.OFFSET_X + col * CONSTS.CELL_SIZE;
                int expectedY = CONSTS.OFFSET_Y + row * CONSTS.CELL_SIZE;
                
                if (Math.abs(x-expectedX) < 1 && Math.abs(y-expectedY) < 1) // 1 = epsilon here
                {
                    for (Node child : sp.getChildren())
                    {
                        if (child instanceof Circle)
                        {
                            Circle circle = (Circle) child;
                            circle.setFill(getVertexColor(targetVertex));
                            if (targetVertex.type.equals(CONSTS.VERTEX_TYPE_UNICORN))
                            {
                                DropShadow ds = new DropShadow();
                                ds.setRadius(CONSTS.VERTEX_RADIUS);
                                ds.setSpread(0.4);
                                ds.setOffsetX(0);
                                ds.setOffsetY(0);
                                ds.setColor(Color.web(CONSTS.COLOR_GOLD));
                                circle.setEffect(ds);
                            }
                            else
                                circle.setEffect(null);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }
}

class CONSTS
{
    public static Font CUSTOM_FONT;

    public static final double PRANS[] = {0, 0.0104, 0.0392, 0.083, 0.1387, 0.2033, 0.2741, 0.3487, 0.4251, 0.5015, 0.5763, 0.6482, 0.7162, 0.7796, 0.8378, 0.8903, 0.937, 0.9779, 1.013, 1.0424, 1.0665, 1.0856, 1.1, 1.1102, 1.1166, 1.1197, 1.1199, 1.1176, 1.1133, 1.1074, 1.1002, 1.0921, 1.0833, 1.0742, 1.065, 1.0559, 1.0471, 1.0386, 1.0307, 1.0233, 1.0166, 1.0106, 1.0053, 1.0007, 0.9968, 0.9936, 0.9909, 0.9889, 0.9874, 0.9864, 0.9858, 0.9856, 0.9857, 0.9861, 0.9867, 0.9875, 0.9884, 0.9894, 0.9905, 0.9916, 0.9927, 0.9938, 0.9948, 0.9958, 0.9967, 0.9976, 0.9983, 0.999, 0.9996, 1.0001, 1.0006, 1.0009, 1.0012, 1.0014, 1.0016, 1.0017, 1.0017, 1.0017, 1.0017, 1.0016, 1.0016, 1.0015, 1.0013, 1.0012, 1.0011, 1.001, 1.0008, 1.0007, 1.0006, 1.0005, 1.0003, 1.0003, 1.0002, 1.0001, 1, 1, 0.9999, 0.9999, 0.9998, 0.9998, 1};

    public static int MAX_PLAYER_COUNT = 4;

    public static final int MAX_WIDTH  = 5;
    public static final int MAX_HEIGHT = 5;

    public static final int WINDOW_WIDTH  = 1000; // pixels
    public static final int WINDOW_HEIGHT = 800;  // pixels
    public static final int CELL_SIZE     = 80;   // pixels
    public static final int VERTEX_RADIUS = 12;   // pixels
    public static final int SECTOR_SIZE   = 80;   // pixels

    public static final int MAP_WIDTH  = (MAX_WIDTH + 1) * CELL_SIZE;
    public static final int MAP_HEIGHT = (MAX_HEIGHT + 1) * CELL_SIZE;
    public static final int OFFSET_X   = (WINDOW_WIDTH - MAP_WIDTH) / 2;
    public static final int OFFSET_Y   = (WINDOW_HEIGHT - MAP_HEIGHT) / 2;

    public static final String SECTOR_TYPE_AI      = "AI HUB";
    public static final String SECTOR_TYPE_FINTECH = "FINTECH";
    public static final String SECTOR_TYPE_CLOUD   = "CLOUD";
    public static final String SECTOR_TYPE_IP      = "IP QUART";
    public static final String SECTOR_TYPE_DATA    = "DATA";
    public static final String SECTOR_TYPE_REGU    = "REGULAT";

    public static final String REWARD_TYPE_AI      = "TALENT";
    public static final String REWARD_TYPE_FINTECH = "CAPITAL";
    public static final String REWARD_TYPE_CLOUD   = "CLOUD";
    public static final String REWARD_TYPE_IP      = "PATENT";
    public static final String REWARD_TYPE_DATA    = "DATA";
    public static final String REWARD_TYPE_REGU    = "NONE";
    
    public static final Map<String, String> SECTOR_REWARDS_HM = new HashMap<String, String>();
    public static final Map<String, String> SECTOR_COLORS_HM  = new HashMap<String, String>();
    

    public static final String VERTEX_TYPE_NONE    = "NONE";
    public static final String VERTEX_TYPE_MVP     = "MVP";
    public static final String VERTEX_TYPE_UNICORN = "UNICORN";
    
    public static final String COLOR_GREEN = "#00ffc8";
    public static final String COLOR_RED   = "#ff0055";
    public static final String COLOR_GOLD  = "#EFBF04";
    
    public static final ArrayList<Map<String, Integer>> MVP_COST     = new ArrayList<Map<String, Integer>>();
    public static final ArrayList<Map<String, Integer>> UNICORN_COST = new ArrayList<Map<String, Integer>>();
    
    public static final String PLAYER_ROLE_NONE      = "NONE";
    public static final String PLAYER_ROLE_CEO       = "CEO";
    public static final String PLAYER_ROLE_NERD_AHH  = "NERD";
    public static final String PLAYER_ROLE_RICH_AHH  = "RICH";
    
    public static final String PLAYER_1_COLOR  = "#112E81";
    public static final String PLAYER_2_COLOR  = "#4647AE";
    public static final String PLAYER_3_COLOR  = "#4382DF";
    public static final String PLAYER_4_COLOR  = "#AACCD6";
    
    static
    {
        try { CUSTOM_FONT = Font.loadFont(new FileInputStream("./assets/font.ttf"), 12); } catch (Exception e) {}

        // HERE
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_AI, REWARD_TYPE_AI);
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_FINTECH, REWARD_TYPE_FINTECH);
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_CLOUD, REWARD_TYPE_CLOUD);
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_IP, REWARD_TYPE_IP);
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_DATA, REWARD_TYPE_DATA);
        SECTOR_REWARDS_HM.put(SECTOR_TYPE_REGU, REWARD_TYPE_REGU);
     
        SECTOR_COLORS_HM.put(SECTOR_TYPE_AI, "#121358");
        SECTOR_COLORS_HM.put(SECTOR_TYPE_FINTECH, "#36ADA3");
        SECTOR_COLORS_HM.put(SECTOR_TYPE_CLOUD, "#2F578A");
        SECTOR_COLORS_HM.put(SECTOR_TYPE_IP, "#232F72");
        SECTOR_COLORS_HM.put(SECTOR_TYPE_DATA, "#640D5F");
        SECTOR_COLORS_HM.put(SECTOR_TYPE_REGU, "#111111");
        
        Map<String, Integer> __mvpCost = new HashMap<String, Integer>();
        __mvpCost.put(REWARD_TYPE_AI, 1);
        __mvpCost.put(REWARD_TYPE_FINTECH, 1);
        __mvpCost.put(REWARD_TYPE_CLOUD, 1);
        __mvpCost.put(REWARD_TYPE_IP, 0);
        __mvpCost.put(REWARD_TYPE_DATA, 1);
        MVP_COST.add(__mvpCost);

        Map<String, Integer> __unicornCost = new HashMap<String, Integer>();
        __unicornCost.put(REWARD_TYPE_AI, 0);
        __unicornCost.put(REWARD_TYPE_FINTECH, 0);
        __unicornCost.put(REWARD_TYPE_CLOUD, 2);
        __unicornCost.put(REWARD_TYPE_IP, 0);
        __unicornCost.put(REWARD_TYPE_DATA, 3);
        UNICORN_COST.add(__unicornCost);
    }
}

class Player
{
    public int idx;

    public int score;
    public int cardLimit;
    
    public int talentCount;
    public int capitalCount;
    public int cloudCount;
    public int patentCount;
    public int dataCount;

    public String role;

    public Player(int i)
    {
        idx = i;

        score = 0;

        cardLimit = 7;

        talentCount = capitalCount = cloudCount = patentCount = dataCount = 10;
        
        role = CONSTS.PLAYER_ROLE_NONE;
    }
}

class Vertex
{
    public int idx;
    public Player owner;
    public String type;

    public Vertex(int i)
    {
        idx = i;
      
        type = CONSTS.VERTEX_TYPE_NONE;
    }
}

class Edge
{
    public int row1;
    public int col1;
    public int row2;
    public int col2;
    public Player owner;
    
    public Edge(int r1, int c1, int r2, int c2)
    {
        row1 = r1;
        col1 = c1;
        row2 = r2;
        col2 = c2;
        
        owner = null;
    }
}

class Sector
{
    public int idx;
    public String type;
    public int dieNum;

    public boolean hasTaxAgent;

    public Sector(int i)
    {
        idx = i;

        String types[] = {CONSTS.SECTOR_TYPE_AI, CONSTS.SECTOR_TYPE_FINTECH, CONSTS.SECTOR_TYPE_CLOUD, CONSTS.SECTOR_TYPE_IP, CONSTS.SECTOR_TYPE_DATA, CONSTS.SECTOR_TYPE_REGU};
        type = types[(int)(Math.random() * types.length)];

        dieNum = (int)(11 * Math.random() + 2);

        hasTaxAgent = false;
    }
}

public class Main extends Application
{
    static Pane rootPane;

    static Player PLAYERS[]      = new Player[CONSTS.MAX_PLAYER_COUNT];
    static int currentPlayerIdx  = 0;
    static int EDGES_FROM_TO[][] = new int[CONSTS.MAX_HEIGHT][CONSTS.MAX_WIDTH];
    static Edge horizontalEdges[][];
    static Edge verticalEdges[][];
    static int HORIZONTAL_EDGE_COUNT;
    static int VERTICAL_EDGE_COUNT;
    static Sector SECTORS[]      = new Sector[CONSTS.MAX_HEIGHT * CONSTS.MAX_HEIGHT];
    static Vertex VERTICES[][]   = new Vertex[CONSTS.MAX_HEIGHT+1][CONSTS.MAX_WIDTH+1];

    static int direction = 1; // 1 = forward, -1 = backward
    static boolean atEnd = false;

    static boolean isInitializationNobat = true;
    static int initTurnsCompleted = 0;
    static int initTotalTurns = 0;
    static Map<Player, List<Vertex>> initSelectedVertices = new HashMap<Player, List<Vertex>>();
    static Vertex pendingInitVertex = null;

    static VBox playerInfoPanels[] = new VBox[4];
    static Text playerScoreTexts[] = new Text[4];
    static VBox playerResourcePanels[] = new VBox[4];

    static int currentDieNum = 0;
    static boolean isSelectingSector = false;
    static Sector selectedTaxSector = null;
    static Sector oldTaxSector = null;

    static boolean isPayingTax = false;
    static Player currentTaxPayer = null;
    static int taxTarget = 0;
    static int currentTaxPaid = 0;
    static Map<String, Integer> taxSelection = new HashMap<String, Integer>();

    static Player selectedTradePlayer = null;
    static Map<String, Integer> currentOffer = new HashMap<String, Integer>();

    static Text diceStatusText;

    static int playerCount;
    static Button diceBtn;
    static Button shopBtn;
    static Button tradeBtn;
    static Button nextBtn;
    static HBox buttonBar;

    static boolean canInteractWithVertices = false;

    // SHOP RELATED
    static int talentPrice  = 4;
    static int cloudPrice   = 4;
    static int patentPrice  = 4;
    static int dataPrice    = 4;

    static boolean talentwasBought  = false;
    static boolean cloudwasBought   = false;
    static boolean patentwasBought  = false;
    static boolean datawasBought    = false;


    static int talentNotBoughtForNRounds  = 0;
    static int cloudNotBoughtForNRounds   = 0;
    static int patentNotBoughtForNRounds  = 0;
    static int dataNotBoughtForNRounds    = 0;
    // END SHOP RELATED

    @Override
    public void start(Stage primaryStage)
    {
        showPlayerSelection(primaryStage);
    }

    public static void main(String args[])
    {
        launch(args);
    }

    // APIs/others
    public static void handleInitializationNobat(Vertex vertex, Edge edge, Player player)
    {
        if (!isInitializationNobat) return;
        
        List<Vertex> playerVertices = initSelectedVertices.get(player);
        if (playerVertices != null && playerVertices.size() >= 2) return;
        
        vertex.owner = player;
        vertex.type = CONSTS.VERTEX_TYPE_MVP;
        edge.owner = player;
        
        if (playerVertices == null)
        {
            playerVertices = new ArrayList<Vertex>();
            initSelectedVertices.put(player, playerVertices);
        }
        playerVertices.add(vertex);
        
        pendingInitVertex = null;
        
        HELPERS.refreshVertexColor(rootPane, vertex);
        refreshEdgesColor();
        updatePlayerInfo();
        
        initTurnsCompleted++;
        
        if (initTurnsCompleted >= initTotalTurns)
            distributeInitResources();
        else
            nextInitTurn();
    }

    public static void nextInitTurn()
    {
        int nextIdx = currentPlayerIdx + direction;
        
        if (nextIdx < 0 || nextIdx >= playerCount)
            direction *= -1;
        else
            currentPlayerIdx = nextIdx;
        
        pendingInitVertex = null;
        
        for (Node node : rootPane.getChildren())
        {
            if (node instanceof StackPane)
            {
                StackPane sp = (StackPane) node;
                sp.setScaleX(1.0);
                sp.setScaleY(1.0);
            }
        }
        
        if (initTurnsCompleted >= initTotalTurns)
            distributeInitResources();
        else
            diceStatusText.setText("⚄: INITIALIZATION - PLAYER " + (currentPlayerIdx + 1) + " CHOOSE VERTEX + EDGE");
    }

    public static void distributeInitResources()
    {
        isInitializationNobat = false;
        System.out.println("INITIALIZATION COMPLETE");
        
        for (Map.Entry<Player, List<Vertex>> entry : initSelectedVertices.entrySet())
        {
            Player player = entry.getKey();
            for (Vertex vertex : entry.getValue())
            {
                int vertexIdx = vertex.idx;
                Sector sector = null;
                for (Sector s : SECTORS)
                {
                    int vertexIndices[] = HELPERS.getVertexIdxFromSectorIdx(s.idx);
                    for (int vIdx : vertexIndices)
                    {
                        if (vIdx == vertexIdx)
                        {
                            sector = s;
                            break;
                        }
                    }
                    if (sector != null) break;
                }
                
                if (sector != null)
                {
                    String rewardType = CONSTS.SECTOR_REWARDS_HM.get(sector.type);
                    switch (rewardType)
                    {
                        case CONSTS.REWARD_TYPE_AI: player.talentCount += 1; break;
                        case CONSTS.REWARD_TYPE_CLOUD: player.cloudCount += 1; break;
                        case CONSTS.REWARD_TYPE_DATA: player.dataCount += 1; break;
                        case CONSTS.REWARD_TYPE_FINTECH: player.capitalCount += 1; break;
                        case CONSTS.REWARD_TYPE_IP: player.patentCount += 1; break;
                        default: break;
                    }
                }
                player.score += 1;
            }
        }
        
        updatePlayerInfo();
        initSelectedVertices.clear();
        initTurnsCompleted = 0;
        atEnd = false;
        direction = 1;

        diceBtn.setDisable(false);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);
        canInteractWithVertices = false;
        
        diceStatusText.setText("⚄: WAITING FOR PLAYER " + (currentPlayerIdx + 1) + " TO ROLL...");
    }
    public static boolean isVertexInteractable(Vertex vertex)
    {
        if (isInitializationNobat)
        {
            if (vertex.owner != null) return false;
            
            Player current = getCurrentPlayer();
            List<Vertex> selected = initSelectedVertices.get(current);
            if (selected != null && selected.size() >= 2) return false;
            
            if (pendingInitVertex != null) return false;
            
            for (int i = 0; i<VERTICES.length; i++)
            {
                for (int j = 0; j<VERTICES[i].length; j++)
                {
                    Vertex owned = VERTICES[i][j];
                    if (owned.owner != null)
                        if (HELPERS.getVertexDistance(vertex.idx, owned.idx) <= 1)
                            return false;
                }
            }
            return true;
        }
        
        if (vertex.owner == getCurrentPlayer())
            return true;
        
        if (vertex.owner == null)
        {
            for (int i = 0; i < VERTICES.length; i++)
            {
                for (int j = 0; j < VERTICES[i].length; j++)
                {
                    Vertex owned = VERTICES[i][j];
                    if (owned.owner != null)
                    {
                        double distance = HELPERS.getVertexDistance(vertex.idx, owned.idx);
                        if (distance <= 1)
                            return false;
                    }
                }
            }
            return true;
        }
        
        return false;
    }
    public static void createPlayerInfoPanels(Pane root)
    {
        String colors[] = {CONSTS.PLAYER_1_COLOR, CONSTS.PLAYER_2_COLOR, CONSTS.PLAYER_3_COLOR, CONSTS.PLAYER_4_COLOR};
        int positions[] = {0, 0,
                        CONSTS.WINDOW_WIDTH - 150, 0,
                        0, CONSTS.WINDOW_HEIGHT - 160,
                        CONSTS.WINDOW_WIDTH - 150, CONSTS.WINDOW_HEIGHT - 160};
        
        for (int i = 0; i<playerCount; i++)
        {
            VBox panel = new VBox(3);
            panel.setAlignment(javafx.geometry.Pos.CENTER);
            panel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: " + colors[i] + "; -fx-border-width: 2; -fx-border-radius: 10;");
            panel.setPrefWidth(150);
            panel.setPrefHeight(85);
            panel.setLayoutX(positions[i * 2]);
            panel.setLayoutY(positions[i * 2 + 1]);
            
            Text nameText = new Text("PLAYER " + (i + 1));
            nameText.setFill(Color.web(colors[i]));
            nameText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 12));
            nameText.setStyle("-fx-font-weight: bold;");
            
            Text scoreText = new Text("SCORE: 0");
            scoreText.setFill(Color.WHITE);
            scoreText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 11));
            playerScoreTexts[i] = scoreText;
            
            Text totalText = new Text("CARDS: " + HELPERS.getCardCount(PLAYERS[i]));
            totalText.setFill(Color.WHITE);
            totalText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 10));
            
            VBox resourceBox = new VBox(2);
            resourceBox.setAlignment(javafx.geometry.Pos.CENTER);
            resourceBox.setStyle("-fx-padding: 2;");
            
            Text talentText = new Text("🎓: " + PLAYERS[i].talentCount);
            talentText.setFill(Color.web(CONSTS.COLOR_GREEN));
            talentText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
            
            Text capitalText = new Text("💰: " + PLAYERS[i].capitalCount);
            capitalText.setFill(Color.web(CONSTS.COLOR_GREEN));
            capitalText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
            
            Text cloudText = new Text("☁️: " + PLAYERS[i].cloudCount);
            cloudText.setFill(Color.web(CONSTS.COLOR_GREEN));
            cloudText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
            
            Text patentText = new Text("📜: " + PLAYERS[i].patentCount);
            patentText.setFill(Color.web(CONSTS.COLOR_GREEN));
            patentText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
            
            Text dataText = new Text("📊: " + PLAYERS[i].dataCount);
            dataText.setFill(Color.web(CONSTS.COLOR_GREEN));
            dataText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
            
            resourceBox.getChildren().addAll(talentText, capitalText, cloudText, patentText, dataText);
            playerResourcePanels[i] = resourceBox;
            
            if (i == currentPlayerIdx)
            {
                resourceBox.setVisible(true);
                panel.setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: " + colors[i] + "; -fx-border-width: 3; -fx-border-radius: 10;");
            }
            else
                resourceBox.setVisible(false);
            
            if (i < playerCount)
            {
                panel.getChildren().addAll(nameText, scoreText, totalText, resourceBox);
                root.getChildren().add(panel);
                playerInfoPanels[i] = panel;
            }
        }
    }
    public static void updatePlayerInfo()
    {
        String colors[] = {CONSTS.PLAYER_1_COLOR, CONSTS.PLAYER_2_COLOR, CONSTS.PLAYER_3_COLOR, CONSTS.PLAYER_4_COLOR};
        
        for (int i = 0; i<playerCount; i++)
        {
            if (playerScoreTexts[i] != null)
                playerScoreTexts[i].setText("SCORE: " + PLAYERS[i].score);
            
            if (playerInfoPanels[i] != null && playerInfoPanels[i].getChildren().size() >= 3)
            {
                if (playerInfoPanels[i].getChildren().get(2) instanceof Text)
                {
                    Text totalText = (Text) playerInfoPanels[i].getChildren().get(2);
                    totalText.setText("TOTAL: " + HELPERS.getCardCount(PLAYERS[i]));
                }
            }
            
            if (playerResourcePanels[i] != null)
            {
                playerResourcePanels[i].getChildren().clear();
                
                Text talentText = new Text("🎓: " + PLAYERS[i].talentCount);
                talentText.setFill(Color.web(CONSTS.COLOR_GREEN));
                talentText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
                
                Text capitalText = new Text("💰: " + PLAYERS[i].capitalCount);
                capitalText.setFill(Color.web(CONSTS.COLOR_GREEN));
                capitalText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
                
                Text cloudText = new Text("☁️: " + PLAYERS[i].cloudCount);
                cloudText.setFill(Color.web(CONSTS.COLOR_GREEN));
                cloudText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
                
                Text patentText = new Text("📜: " + PLAYERS[i].patentCount);
                patentText.setFill(Color.web(CONSTS.COLOR_GREEN));
                patentText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
                
                Text dataText = new Text("📊: " + PLAYERS[i].dataCount);
                dataText.setFill(Color.web(CONSTS.COLOR_GREEN));
                dataText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 9));
                
                playerResourcePanels[i].getChildren().addAll(talentText, capitalText, cloudText, patentText, dataText);
                playerResourcePanels[i].setVisible(i == currentPlayerIdx);
                
                if (playerInfoPanels[i] != null)
                {
                    if (i == currentPlayerIdx)
                        playerInfoPanels[i].setStyle("-fx-background-color: rgba(0,0,0,0.8); -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: " + colors[i] + "; -fx-border-width: 3; -fx-border-radius: 10;");
                    else
                        playerInfoPanels[i].setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: " + colors[i] + "; -fx-border-width: 2; -fx-border-radius: 10;");
                }
            }
        }
    }
    public static void startSectorSelection()
    {
        if (isPayingTax) return;
        isSelectingSector = true;
        canInteractWithVertices = false;
        diceBtn.setDisable(true);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);

        isSelectingSector = true;
        canInteractWithVertices = false;

        diceBtn.setDisable(true);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);
        
        for (Node node : rootPane.getChildren())
        {
            if (node instanceof StackPane)
            {
                StackPane sp = (StackPane) node;
                for (Node child : sp.getChildren())
                {
                    if (child instanceof Rectangle)
                    {
                        Rectangle rect = (Rectangle) child;
                        if (rect.getUserData() == null)
                            rect.setUserData(rect.getStroke());
                        
                        // Find the sector
                        int sectorIndex = -1;
                        for (int idx = 0; idx < SECTORS.length; idx++)
                        {
                            if (SECTORS[idx] != null)
                            {
                                int vertexIndices[] = HELPERS.getVertexIdxFromSectorIdx(idx);
                                boolean sectorHasOwnedVertex = false;
                                for (int vIdx : vertexIndices)
                                {
                                    int row = vIdx / (CONSTS.MAX_WIDTH + 1);
                                    int col = vIdx % (CONSTS.MAX_WIDTH + 1);
                                    Vertex v = VERTICES[row][col];
                                    if (v.owner != null)
                                    {
                                        sectorHasOwnedVertex = true;
                                        break;
                                    }
                                }
                                if (sectorHasOwnedVertex && !SECTORS[idx].hasTaxAgent)
                                {
                                    sectorIndex = idx;
                                    break;
                                }
                            }
                        }
                        
                        if (sectorIndex != -1)
                        {
                            rect.setStroke(Color.GREEN);
                            rect.setStrokeWidth(3);
                        }
                    }
                }
            }
        }
    }

    public static void selectSector(Sector sector)
    {
        if (!isSelectingSector) return;
        
        boolean hasOwnedVertex = false;
        int vertexIndices[] = HELPERS.getVertexIdxFromSectorIdx(sector.idx);
        for (int vIdx : vertexIndices)
        {
            int row = vIdx/(CONSTS.MAX_WIDTH+1);
            int col = vIdx%(CONSTS.MAX_WIDTH+1);
            Vertex v = VERTICES[row][col];
            if (v.owner != null)
            {
                hasOwnedVertex = true;
                break;
            }
        }
        
        if (!hasOwnedVertex) return;
        if (sector.hasTaxAgent) return;
        
        selectedTaxSector = sector;
        isSelectingSector = false;
        
        for (Node node : rootPane.getChildren())
        {
            if (node instanceof StackPane)
            {
                StackPane sp = (StackPane) node;
                for (Node child : sp.getChildren())
                {
                    if (child instanceof Rectangle)
                    {
                        Rectangle rect = (Rectangle) child;
                        if (rect.getUserData() != null && (rect.getUserData() instanceof Color))
                        {
                            rect.setStroke((Color) rect.getUserData());
                            rect.setStrokeWidth(2);
                        }
                    }
                }
            }
        }
        
        applyTaxAgent(selectedTaxSector);
    }

    public static void applyTaxAgent(Sector sector)
    {
        oldTaxSector = null;
        for (Sector s : SECTORS)
        {
            if (s.hasTaxAgent)
            {
                oldTaxSector = s;
                s.hasTaxAgent = false;
                break;
            }
        }
        
        sector.hasTaxAgent = true;
        
        updatePlayerInfo();
        refreshSectors();

        canInteractWithVertices = true;
        diceBtn.setDisable(true);
        shopBtn.setDisable(false);
        tradeBtn.setDisable(false);
        nextBtn.setDisable(false);
        
        diceStatusText.setText("⚄: 7");

        updatePlayerInfo();
        processDiceRoll();
    }
    public static void refreshSectors()
    {
        List<Node> toRemove = new ArrayList<Node>();
        List<Node> verticesAndEdges = new ArrayList<Node>();
        
        for (Node node : rootPane.getChildren())
        {
            if (node instanceof StackPane)
            {
                StackPane sp = (StackPane) node;
                boolean isSector = false;
                boolean isVertex = false;
                
                for (Node child : sp.getChildren())
                {
                    if (child instanceof Rectangle)
                        isSector = true;
                    if (child instanceof Circle)
                        isVertex = true;
                }
                
                if (isSector && !isVertex)
                    toRemove.add(node);
                else if (isVertex)
                    verticesAndEdges.add(node);
            }
            else if (node instanceof Line)
                verticesAndEdges.add(node);
        }
        
        rootPane.getChildren().removeAll(toRemove);
        HELPERS.drawSectors(rootPane, SECTORS);
        
        for (Node node : verticesAndEdges)
        {
            rootPane.getChildren().remove(node);
            rootPane.getChildren().add(node);
        }
    }
    public static void showTradeOffer(Player targetPlayer, Player currentPlayer)
    {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = rootPane.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(450);
        dialog.setPrefHeight(500);
        
        Text titleText = new Text("PLAYER " + (targetPlayer.idx + 1) + " - VIEW OFFER");
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        titleText.setFill(Color.WHITE);
        titleText.setStyle("-fx-font-weight: bold;");
        
        Text fromText = new Text("FROM PLAYER " + (currentPlayer.idx + 1));
        fromText.setFill(Color.web(CONSTS.COLOR_GREEN));
        fromText.setFont(Font.font(14));
        
        VBox giveBox = new VBox(5);
        giveBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text giveLabel = new Text("GET:");
        giveLabel.setFill(Color.WHITE);
        giveLabel.setFont(Font.font(14));
        giveLabel.setStyle("-fx-font-weight: bold;");
        
        VBox giveList = new VBox(3);
        giveList.setAlignment(javafx.geometry.Pos.CENTER);
        
        String resources[] = {"TALENT", "CAPITAL", "CLOUD", "PATENT", "DATA"};
        for (int i = 0; i<resources.length; i++)
        {
            int amount = currentOffer.get(resources[i] + "_GIVE");
            if (amount > 0)
            {
                Text giveItem = new Text(resources[i] + ": " + amount);
                giveItem.setFill(Color.WHITE);
                giveItem.setFont(Font.font(12));
                giveList.getChildren().add(giveItem);
            }
        }
        if (giveList.getChildren().isEmpty())
        {
            Text none = new Text("NOTHING");
            none.setFill(Color.LIGHTGRAY);
            giveList.getChildren().add(none);
        }
        giveBox.getChildren().addAll(giveLabel, giveList);
        
        VBox receiveBox = new VBox(5);
        receiveBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text receiveLabel = new Text("GIVE:");
        receiveLabel.setFill(Color.WHITE);
        receiveLabel.setFont(Font.font(14));
        receiveLabel.setStyle("-fx-font-weight: bold;");
        
        VBox receiveList = new VBox(3);
        receiveList.setAlignment(javafx.geometry.Pos.CENTER);
        
        for (int i = 0; i < resources.length; i++)
        {
            int amount = currentOffer.get(resources[i] + "_get");
            if (amount > 0)
            {
                Text receiveItem = new Text(resources[i] + ": " + amount);
                receiveItem.setFill(Color.WHITE);
                receiveItem.setFont(Font.font(12));
                receiveList.getChildren().add(receiveItem);
            }
        }
        if (receiveList.getChildren().isEmpty())
        {
            Text none = new Text("NOTHING");
            none.setFill(Color.LIGHTGRAY);
            receiveList.getChildren().add(none);
        }
        receiveBox.getChildren().addAll(receiveLabel, receiveList);
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button acceptBtn = new Button("ACCEPT");
        acceptBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        acceptBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(acceptBtn, 1.2, 1000).play());
        acceptBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(acceptBtn, 1.0, 1000).play());
        acceptBtn.setCursor(javafx.scene.Cursor.HAND);
        
        Button rejectBtn = new Button("REJECT");
        rejectBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        rejectBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(rejectBtn, 1.2, 1000).play());
        rejectBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(rejectBtn, 1.0, 1000).play());
        rejectBtn.setCursor(javafx.scene.Cursor.HAND);
        
        acceptBtn.setOnAction(e -> {
            boolean hasEnough = true;
            if (currentOffer.get("TALENT_get") > targetPlayer.talentCount) hasEnough = false;
            if (currentOffer.get("CAPITAL_get") > targetPlayer.capitalCount) hasEnough = false;
            if (currentOffer.get("CLOUD_get") > targetPlayer.cloudCount) hasEnough = false;
            if (currentOffer.get("PATENT_get") > targetPlayer.patentCount) hasEnough = false;
            if (currentOffer.get("DATA_get") > targetPlayer.dataCount) hasEnough = false;
            
            if (!hasEnough)
            {
                Text error = new Text("GET YO BROKE AHH OUTTA HERE");
                error.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(error);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(error));
                pause.play();
                return;
            }
            
            for (String resource : resources)
            {
                int giveAmount = currentOffer.get(resource + "_GIVE");
                int receiveAmount = currentOffer.get(resource + "_get");
                
                switch (resource)
                {
                    case "TALENT": currentPlayer.talentCount -= giveAmount; break;
                    case "CAPITAL": currentPlayer.capitalCount -= giveAmount; break;
                    case "CLOUD": currentPlayer.cloudCount -= giveAmount; break;
                    case "PATENT": currentPlayer.patentCount -= giveAmount; break;
                    case "DATA": currentPlayer.dataCount -= giveAmount; break;
                }
                
                switch (resource)
                {
                    case "TALENT": targetPlayer.talentCount -= receiveAmount; break;
                    case "CAPITAL": targetPlayer.capitalCount -= receiveAmount; break;
                    case "CLOUD": targetPlayer.cloudCount -= receiveAmount; break;
                    case "PATENT": targetPlayer.patentCount -= receiveAmount; break;
                    case "DATA": targetPlayer.dataCount -= receiveAmount; break;
                }
                
                switch (resource)
                {
                    case "TALENT": currentPlayer.talentCount += receiveAmount; break;
                    case "CAPITAL": currentPlayer.capitalCount += receiveAmount; break;
                    case "CLOUD": currentPlayer.cloudCount += receiveAmount; break;
                    case "PATENT": currentPlayer.patentCount += receiveAmount; break;
                    case "DATA": currentPlayer.dataCount += receiveAmount; break;
                }
                
                switch (resource)
                {
                    case "TALENT": targetPlayer.talentCount += giveAmount; break;
                    case "CAPITAL": targetPlayer.capitalCount += giveAmount; break;
                    case "CLOUD": targetPlayer.cloudCount += giveAmount; break;
                    case "PATENT": targetPlayer.patentCount += giveAmount; break;
                    case "DATA": targetPlayer.dataCount += giveAmount; break;
                }
            }
            
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
            
            Pane successOverlay = new Pane();
            successOverlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
            VBox successDialog = new VBox(15);
            successDialog.setAlignment(javafx.geometry.Pos.CENTER);
            successDialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25;");
            successDialog.setPrefWidth(300);
            successDialog.setPrefHeight(150);
            Text successText = new Text("TRADE COMPLETED");
            successText.setFill(Color.web(CONSTS.COLOR_GREEN));
            successText.setFont(Font.font(16));
            successText.setStyle("-fx-font-weight: bold;");
            Button okBtn = new Button("COOL");
            okBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
            okBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.2, 1000).play());
            okBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.0, 1000).play());
            okBtn.setOnAction(ev -> {
                rootPane.getChildren().remove(successOverlay);
                updatePlayerInfo();
            });
            okBtn.setCursor(javafx.scene.Cursor.HAND);
            successDialog.getChildren().addAll(successText, okBtn);
            successDialog.setLayoutX((CONSTS.WINDOW_WIDTH - successDialog.getPrefWidth()) / 2);
            successDialog.setLayoutY((CONSTS.WINDOW_HEIGHT - successDialog.getPrefHeight()) / 2);
            successOverlay.getChildren().add(successDialog);
            rootPane.getChildren().add(successOverlay);
        });
        
        rejectBtn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
            
            Pane rejectOverlay = new Pane();
            rejectOverlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
            VBox rejectDialog = new VBox(15);
            rejectDialog.setAlignment(javafx.geometry.Pos.CENTER);
            rejectDialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25;");
            rejectDialog.setPrefWidth(300);
            rejectDialog.setPrefHeight(150);
            Text rejectText = new Text("TRADE BLOWN");
            rejectText.setFill(Color.web(CONSTS.COLOR_RED));
            rejectText.setFont(Font.font(16));
            rejectText.setStyle("-fx-font-weight: bold;");
            Button okBtn = new Button("😭");
            okBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
            okBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.2, 1000).play());
            okBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.0, 1000).play());
            okBtn.setOnAction(ev -> rootPane.getChildren().remove(rejectOverlay));
            okBtn.setCursor(javafx.scene.Cursor.HAND);
            rejectDialog.getChildren().addAll(rejectText, okBtn);
            rejectDialog.setLayoutX((CONSTS.WINDOW_WIDTH - rejectDialog.getPrefWidth()) / 2);
            rejectDialog.setLayoutY((CONSTS.WINDOW_HEIGHT - rejectDialog.getPrefHeight()) / 2);
            rejectOverlay.getChildren().add(rejectDialog);
            rootPane.getChildren().add(rejectOverlay);
        });
        
        buttonBox.getChildren().addAll(acceptBtn, rejectBtn);
        
        dialog.getChildren().addAll(titleText, fromText, giveBox, receiveBox, buttonBox);
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        rootPane.getChildren().add(overlay);
    }
    public static boolean canBuyEdge(Edge edge, Player player)
    {
        if (isInitializationNobat)
        {
            if (pendingInitVertex == null) return false;
            List<Vertex> selected = initSelectedVertices.get(player);
            if (selected != null && selected.size() >= 2) return false;
            
            Vertex selectedVertex = pendingInitVertex;
            
            int v1 = selectedVertex.idx;
            int cols = CONSTS.MAX_WIDTH+1;
            int row1 = v1/cols;
            int col1 = v1%cols;
            
            if ((edge.row1 == row1 && edge.col1 == col1) || (edge.row2 == row1 && edge.col2 == col1))
            {
                if (edge.owner != null) return false;
                
                if (edge.row1 == edge.row2)
                {
                    if (edge.col1 > 0)
                    {
                        Edge leftEdge = horizontalEdges[edge.row1][edge.col1-1];
                        if (leftEdge.owner != null) return false;
                    }
                    if (edge.col2 < CONSTS.MAX_WIDTH)
                    {
                        Edge rightEdge = horizontalEdges[edge.row1][edge.col1+1];
                        if (rightEdge.owner != null) return false;
                    }
                    if (edge.row1 > 0)
                    {
                        Edge topLeftVertical = verticalEdges[edge.row1-1][edge.col1];
                        if (topLeftVertical.owner != null) return false;

                        Edge topRightVertical = verticalEdges[edge.row1-1][edge.col2];
                        if (topRightVertical.owner != null) return false;
                    }
                    if (edge.row1 < CONSTS.MAX_HEIGHT)
                    {
                        Edge bottomLeftVertical = verticalEdges[edge.row1][edge.col1];
                        if (bottomLeftVertical.owner != null) return false;

                        Edge bottomRightVertical = verticalEdges[edge.row1][edge.col2];
                        if (bottomRightVertical.owner != null) return false;
                    }
                }
                else
                {
                    if (edge.row1 > 0)
                    {
                        Edge aboveEdge = verticalEdges[edge.row1-1][edge.col1];
                        if (aboveEdge.owner != null) return false;
                    }
                    if (edge.row2 < CONSTS.MAX_HEIGHT)
                    {
                        Edge belowEdge = verticalEdges[edge.row1+1][edge.col1];
                        if (belowEdge.owner != null) return false;
                    }
                    if (edge.col1 > 0)
                    {
                        Edge leftTopHorizontal = horizontalEdges[edge.row1][edge.col1-1];
                        if (leftTopHorizontal.owner != null) return false;

                        Edge leftBottomHorizontal = horizontalEdges[edge.row2][edge.col1-1];
                        if (leftBottomHorizontal.owner != null) return false;
                    }
                    if (edge.col1 < CONSTS.MAX_WIDTH)
                    {
                        Edge rightTopHorizontal = horizontalEdges[edge.row1][edge.col1];
                        if (rightTopHorizontal.owner != null) return false;

                        Edge rightBottomHorizontal = horizontalEdges[edge.row2][edge.col1];
                        if (rightBottomHorizontal.owner != null) return false;
                    }
                }
                
                return true;
            }
            return false;
        }

        int vertices[][] = {{edge.row1, edge.col1}, {edge.row2, edge.col2}};
        
        for (int v[] : vertices)
        {
            int r = v[0];
            int c = v[1];
            if (r >= 0 && r < VERTICES.length && c >= 0 && c < VERTICES[0].length)
            {
                Vertex vertex = VERTICES[r][c];
                if (vertex.owner == player && (vertex.type.equals(CONSTS.VERTEX_TYPE_MVP) || vertex.type.equals(CONSTS.VERTEX_TYPE_UNICORN)))
                    return true;
            }
        }
        
        // horizontal
        if (edge.row1 == edge.row2)
        {
            if (edge.col1 > 0)
            {
                Edge leftEdge = horizontalEdges[edge.row1][edge.col1-1];
                if (leftEdge.owner == player)
                    return true;
            }
            if (edge.col2 < CONSTS.MAX_WIDTH)
            {
                Edge rightEdge = horizontalEdges[edge.row1][edge.col1+1];
                if (rightEdge.owner == player)
                    return true;
            }
            if (edge.row1 > 0)
            {
                Edge topLeftVertical = verticalEdges[edge.row1-1][edge.col1];
                if (topLeftVertical.owner == player)
                    return true;
                Edge topRightVertical = verticalEdges[edge.row1-1][edge.col2];
                if (topRightVertical.owner == player)
                    return true;
            }
            if (edge.row1 < CONSTS.MAX_HEIGHT)
            {
                Edge bottomLeftVertical = verticalEdges[edge.row1][edge.col1];
                if (bottomLeftVertical.owner == player)
                    return true;
                Edge bottomRightVertical = verticalEdges[edge.row1][edge.col2];
                if (bottomRightVertical.owner == player)
                    return true;
            }
        }
        // vertical
        else
        {
            if (edge.row1 > 0)
            {
                Edge aboveEdge = verticalEdges[edge.row1-1][edge.col1];
                if (aboveEdge.owner == player)
                    return true;
            }
            if (edge.row2 < CONSTS.MAX_HEIGHT)
            {
                Edge belowEdge = verticalEdges[edge.row1+1][edge.col1];
                if (belowEdge.owner == player)
                    return true;
            }
            if (edge.col1 > 0)
            {
                Edge leftTopHorizontal = horizontalEdges[edge.row1][edge.col1-1];
                if (leftTopHorizontal.owner == player)
                    return true;
                Edge leftBottomHorizontal = horizontalEdges[edge.row2][edge.col1-1];
                if (leftBottomHorizontal.owner == player)
                    return true;
            }
            if (edge.col1 < CONSTS.MAX_WIDTH)
            {
                Edge rightTopHorizontal = horizontalEdges[edge.row1][edge.col1];
                if (rightTopHorizontal.owner == player)
                    return true;
                Edge rightBottomHorizontal = horizontalEdges[edge.row2][edge.col1];
                if (rightBottomHorizontal.owner == player)
                    return true;
            }
        }
        
        return false;
    }

    public static void showEdgePurchaseDialog(Pane root, Edge edge, Player player)
    {
        if (isInitializationNobat)
        {
            if (pendingInitVertex != null && canBuyEdge(edge, player))
            {
                handleInitializationNobat(pendingInitVertex, edge, player);
                pendingInitVertex = null;
            }
            return;
        }
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = rootPane.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(300);
        dialog.setPrefHeight(200);
        
        Text titleText = new Text("TURN TO PARTNERSHIP");
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        titleText.setFill(Color.WHITE);
        titleText.setStyle("-fx-font-weight: bold;");
        
        VBox costBox = new VBox(5);
        costBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text costTitle = new Text("COSTS:");
        costTitle.setFill(Color.WHITE);
        costTitle.setFont(Font.font(14));
        
        VBox resourceList = new VBox(3);
        resourceList.setAlignment(javafx.geometry.Pos.CENTER);
        
        Text capitalText = new Text("CAPITAL: " + player.capitalCount + "/1");
        capitalText.setFill(player.capitalCount >= 1 ? Color.web(CONSTS.COLOR_GREEN) : Color.web(CONSTS.COLOR_RED));
        capitalText.setFont(Font.font(12));
        
        Text patentText = new Text("PATENT: " + player.patentCount + "/1");
        patentText.setFill(player.patentCount >= 1 ? Color.web(CONSTS.COLOR_GREEN) : Color.web(CONSTS.COLOR_RED));
        patentText.setFont(Font.font(12));
        
        resourceList.getChildren().addAll(capitalText, patentText);
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button purchaseBtn = new Button("PAY");
        purchaseBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_GREEN+"; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        purchaseBtn.setCursor(javafx.scene.Cursor.HAND);
        purchaseBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(purchaseBtn, 1.2, 800).play());
        purchaseBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(purchaseBtn, 1.0, 800).play());
        purchaseBtn.setOnAction(e -> {
            if (player.capitalCount >= 1 && player.patentCount >= 1)
            {
                player.capitalCount -= 1;
                player.patentCount -= 1;
                edge.owner = player;
                
                rootPane.getChildren().remove(overlay);
                rootPane.setEffect(null);
                refreshEdgesColor();
            }
            else
            {
                Text errorMsg = new Text("GET YO BROKE AHH OUTTA HERE!");
                errorMsg.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(errorMsg);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(errorMsg));
                pause.play();
            }
            updatePlayerInfo();
        });
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.setStyle("-fx-background-color: "+CONSTS.COLOR_RED+"; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        cancelBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(cancelBtn, 1.2, 800).play());
        cancelBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(cancelBtn, 1.0, 800).play());
        cancelBtn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
            updatePlayerInfo();
        });
        
        buttonBox.getChildren().addAll(purchaseBtn, cancelBtn);
        costBox.getChildren().addAll(costTitle, resourceList);
        dialog.getChildren().addAll(titleText, costBox, buttonBox);
        
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        rootPane.getChildren().add(overlay);
    }
    public static void refreshEdgesColor()
    {
        for (Node node : rootPane.getChildren())
        {
            if (node instanceof Line)
            {
                Line line = (Line) node;
                Object userData = line.getUserData();
                if (userData instanceof Edge)
                {
                    Edge edge = (Edge) userData;
                    line.setStroke(HELPERS.getEdgeColor(edge));
                }
            }
        }
    }
    public static void showPlayerSelection(Stage primaryStage)
    {
        Pane selectionPane = new Pane();
        selectionPane.setStyle("-fx-background-color: #000000;");
        
        VBox selectionBox = new VBox(20);
        selectionBox.setAlignment(javafx.geometry.Pos.CENTER);
        selectionBox.setLayoutX((CONSTS.WINDOW_WIDTH - 300) / 2);
        selectionBox.setLayoutY((CONSTS.WINDOW_HEIGHT - 200) / 2);
        selectionBox.setPrefWidth(300);
        selectionBox.setPrefHeight(200);
        selectionBox.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        
        Text title = new Text("SELECT NUMBER OF PLAYERS");
        title.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup group = new ToggleGroup();
        
        RadioButton twoPlayer = new RadioButton("2 PLAYERS");
        twoPlayer.setToggleGroup(group);
        twoPlayer.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        twoPlayer.setCursor(javafx.scene.Cursor.HAND);
        
        RadioButton threePlayer = new RadioButton("3 PLAYERS");
        threePlayer.setToggleGroup(group);
        threePlayer.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        threePlayer.setCursor(javafx.scene.Cursor.HAND);
        
        RadioButton fourPlayer = new RadioButton("4 PLAYERS");
        fourPlayer.setToggleGroup(group);
        fourPlayer.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        fourPlayer.setCursor(javafx.scene.Cursor.HAND);
        
        Button startBtn = new Button("START GAME");
        startBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        startBtn.setCursor(javafx.scene.Cursor.HAND);
        startBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(startBtn, 1.2, 1000).play());
        startBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(startBtn, 1.0, 1000).play());
        
        startBtn.setOnAction(e -> {
            if (twoPlayer.isSelected())
                playerCount = 2;
            else if (threePlayer.isSelected())
                playerCount = 3;
            else if (fourPlayer.isSelected())
                playerCount = 4;
            else
                playerCount = 2;
            
            for (int i = 0; i<playerCount; i++)
                PLAYERS[i] = new Player(i);
            
            currentPlayerIdx = 0;
            showRoleSelection(primaryStage);
        });
        
        selectionBox.getChildren().addAll(title, twoPlayer, threePlayer, fourPlayer, startBtn);
        selectionPane.getChildren().add(selectionBox);
        
        Scene selectionScene = new Scene(selectionPane, CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        primaryStage.setScene(selectionScene);
        primaryStage.setTitle("RAGS TO RICHES");
        primaryStage.show();
    }
    public static void showRoleSelection(Stage primaryStage)
    {
        Pane rolePane = new Pane();
        rolePane.setStyle("-fx-background-color: #000000;");
        
        VBox roleBox = new VBox(20);
        roleBox.setAlignment(javafx.geometry.Pos.CENTER);
        roleBox.setLayoutX((CONSTS.WINDOW_WIDTH - 400) / 2);
        roleBox.setLayoutY((CONSTS.WINDOW_HEIGHT - 300) / 2);
        roleBox.setPrefWidth(400);
        roleBox.setPrefHeight(300);
        roleBox.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        
        Text title = new Text("PLAYER " + (currentPlayerIdx + 1) + " - SELECT YOUR ROLE");
        title.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");
        
        ToggleGroup roleGroup = new ToggleGroup();
        
        RadioButton ceoBtn = new RadioButton("CEO   - Gets 3:1 ratio in market                          ");
        ceoBtn.setToggleGroup(roleGroup);
        ceoBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        ceoBtn.setCursor(javafx.scene.Cursor.HAND);
        ceoBtn.setDisable(false);
        
        RadioButton nerdBtn = new RadioButton("NERD - Pays 1 CLOUD for UNICORN upgrades  ");
        nerdBtn.setToggleGroup(roleGroup);
        nerdBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        nerdBtn.setCursor(javafx.scene.Cursor.HAND);
        
        RadioButton richBtn = new RadioButton("RICH  - Starts with 2 CAPITALs and a limit of 9  ");
        richBtn.setToggleGroup(roleGroup);
        richBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        richBtn.setCursor(javafx.scene.Cursor.HAND);
        
        RadioButton noneBtn = new RadioButton("NONE");
        noneBtn.setToggleGroup(roleGroup);
        noneBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        noneBtn.setCursor(javafx.scene.Cursor.HAND);
        
        boolean ceoTaken = false;
        boolean nerdTaken = false;
        boolean richTaken = false;
        
        for (int i = 0; i<currentPlayerIdx; i++)
        {
            if (PLAYERS[i].role.equals(CONSTS.PLAYER_ROLE_CEO))
                ceoTaken = true;
            else if (PLAYERS[i].role.equals(CONSTS.PLAYER_ROLE_NERD_AHH))
                nerdTaken = true;
            else if (PLAYERS[i].role.equals(CONSTS.PLAYER_ROLE_RICH_AHH))
                richTaken = true;
        }
        
        ceoBtn.setDisable(ceoTaken);
        nerdBtn.setDisable(nerdTaken);
        richBtn.setDisable(richTaken);
        
        Button confirmBtn = new Button("CONFIRM ROLE");
        confirmBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        confirmBtn.setCursor(javafx.scene.Cursor.HAND);
        confirmBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(confirmBtn, 1.2, 1000).play());
        confirmBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(confirmBtn, 1.0, 1000).play());
        
        confirmBtn.setOnAction(e -> {
            String selectedRole = null;
            if (ceoBtn.isSelected())
                selectedRole = CONSTS.PLAYER_ROLE_CEO;
            else if (nerdBtn.isSelected())
                selectedRole = CONSTS.PLAYER_ROLE_NERD_AHH;
            else if (richBtn.isSelected())
                selectedRole = CONSTS.PLAYER_ROLE_RICH_AHH;
            else if (noneBtn.isSelected())
                selectedRole = CONSTS.PLAYER_ROLE_NONE;
            
            if (selectedRole == null)
            {
                Text error = new Text("SELECT A ROLE");
                error.setFill(Color.web(CONSTS.COLOR_RED));
                roleBox.getChildren().add(error);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> roleBox.getChildren().remove(error));
                pause.play();
                return;
            }
            
            PLAYERS[currentPlayerIdx].role = selectedRole;
            
            switch (selectedRole)
            {
                case CONSTS.PLAYER_ROLE_CEO:
                    PLAYERS[currentPlayerIdx].score = -1;
                    break;
                case CONSTS.PLAYER_ROLE_NERD_AHH:
                    PLAYERS[currentPlayerIdx].score = -1;
                    break;
                case CONSTS.PLAYER_ROLE_RICH_AHH:
                    PLAYERS[currentPlayerIdx].score = -1;
                    PLAYERS[currentPlayerIdx].capitalCount = 2;
                    PLAYERS[currentPlayerIdx].cardLimit = 9;
                    break;
                case CONSTS.PLAYER_ROLE_NONE:
                    break;
            }
            
            currentPlayerIdx++;
            
            if (currentPlayerIdx<playerCount)
                showRoleSelection(primaryStage);
            else
            {
                currentPlayerIdx = 0;
                initGame(primaryStage);
            }
        });
        
        roleBox.getChildren().addAll(title, ceoBtn, nerdBtn, richBtn, noneBtn, confirmBtn);
        
        rolePane.getChildren().add(roleBox);
        
        Scene roleScene = new Scene(rolePane, CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        primaryStage.setScene(roleScene);
        primaryStage.show();
    }
    public static void initGame(Stage primaryStage)
    {
        // INIT EDGES
        HORIZONTAL_EDGE_COUNT = CONSTS.MAX_HEIGHT;
        VERTICAL_EDGE_COUNT = CONSTS.MAX_WIDTH;
        horizontalEdges = new Edge[CONSTS.MAX_HEIGHT+1][CONSTS.MAX_WIDTH];
        verticalEdges = new Edge[CONSTS.MAX_HEIGHT][CONSTS.MAX_WIDTH+1];

        for (int i = 0; i<=CONSTS.MAX_HEIGHT; i++)
            for (int j = 0; j<CONSTS.MAX_WIDTH; j++)
                horizontalEdges[i][j] = new Edge(i, j, i, j + 1);

        for (int i = 0; i<CONSTS.MAX_HEIGHT; i++)
            for (int j = 0; j<=CONSTS.MAX_WIDTH; j++)
                verticalEdges[i][j] = new Edge(i, j, i + 1, j);
        // END INIT EDGES
        
        // INIT SECTORS
        SECTORS = new Sector[CONSTS.MAX_HEIGHT*CONSTS.MAX_HEIGHT];
        for (int i = 0; i<CONSTS.MAX_HEIGHT*CONSTS.MAX_HEIGHT; i++)
            SECTORS[i] = new Sector(i);
        SECTORS[new Random().nextInt(SECTORS.length)].hasTaxAgent = true;
        // END INIT SECTORS
        
        // INIT VERTICES
        VERTICES = new Vertex[CONSTS.MAX_HEIGHT + 1][CONSTS.MAX_WIDTH + 1];
        int count = 0;
        for (int i = 0; i<CONSTS.MAX_HEIGHT + 1; i++)
            for (int j = 0; j<CONSTS.MAX_WIDTH + 1; j++)
                VERTICES[i][j] = new Vertex(count++);
        // END INIT VERTICES
        
        // Reset these dumbos
        currentPlayerIdx = 0;
        direction = 1;
        atEnd = false;
        isInitializationNobat = true;
        initTurnsCompleted = 0;
        initSelectedVertices.clear();
        initTotalTurns = playerCount * 2;
        talentPrice = cloudPrice = patentPrice = dataPrice = 4;
        talentwasBought = cloudwasBought = patentwasBought = datawasBought = false;
        talentNotBoughtForNRounds = cloudNotBoughtForNRounds = patentNotBoughtForNRounds = dataNotBoughtForNRounds = 0;
        canInteractWithVertices = false;
        
        // Start
        Pane root = new Pane();
        root.setStyle("-fx-background-color: #000000;");
        
        HELPERS.drawSectors(root, SECTORS);
        HELPERS.drawEdges(root, horizontalEdges, verticalEdges);
        HELPERS.drawVertices(root, VERTICES);

        HBox buttonBar = new HBox(20);
        buttonBar.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBar.setLayoutX(0);
        buttonBar.setLayoutY(CONSTS.WINDOW_HEIGHT - 80);
        buttonBar.setPrefWidth(CONSTS.WINDOW_WIDTH);
        buttonBar.setStyle("-fx-background-color: #000000; -fx-padding: 10;");
        
        diceBtn = new Button("⚄");
        diceBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 50; -fx-min-height: 50; -fx-max-width: 50; -fx-max-height: 50; -fx-background-radius: 25; -fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        diceBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(diceBtn, 1.2, 1000).play());
        diceBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(diceBtn, 1.0, 1000).play());
        diceBtn.setOnAction(e -> rollDice());
        
        shopBtn = new Button("🏪");
        shopBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 50; -fx-min-height: 50; -fx-max-width: 50; -fx-max-height: 50; -fx-background-radius: 25; -fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        shopBtn.setDisable(true);
        shopBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(shopBtn, 1.2, 1000).play());
        shopBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(shopBtn, 1.0, 1000).play());
        shopBtn.setOnAction(e -> openShop(getCurrentPlayer()));
        
        tradeBtn = new Button("🤝");
        tradeBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 50; -fx-min-height: 50; -fx-max-width: 50; -fx-max-height: 50; -fx-background-radius: 25; -fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        tradeBtn.setDisable(true);
        tradeBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(tradeBtn, 1.2, 1000).play());
        tradeBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(tradeBtn, 1.0, 1000).play());
        tradeBtn.setOnAction(e -> openTrade());
        
        nextBtn = new Button("✅");
        nextBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 50; -fx-min-height: 50; -fx-max-width: 50; -fx-max-height: 50; -fx-background-radius: 25; -fx-background-color: #444; -fx-text-fill: white; -fx-cursor: hand;");
        nextBtn.setDisable(true);
        nextBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(nextBtn, 1.2, 1000).play());
        nextBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(nextBtn, 1.0, 1000).play());
        nextBtn.setOnAction(e -> nextTurn());
        
        buttonBar.getChildren().addAll(diceBtn, shopBtn, tradeBtn, nextBtn);
        root.getChildren().add(buttonBar);

        createDiceStatusDisplay(root);
        createPlayerInfoPanels(root);
        
        
        diceStatusText.setText("⚄: INITIALIZATION - PLAYER 1 CHOOSE VERTEX + EDGE");
        diceBtn.setDisable(true);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);
        canInteractWithVertices = true;
        
        rootPane = root;

        Scene scene = new Scene(root, CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static Player getCurrentPlayer()
    {
        return PLAYERS[currentPlayerIdx];
    }
    public static void updateButtonsAfterRoll()
    {
        diceBtn.setDisable(true);
        shopBtn.setDisable(false);
        tradeBtn.setDisable(false);
        nextBtn.setDisable(false);
        canInteractWithVertices = true;
    }

    public static void updateButtonsAfterNext()
    {
        diceBtn.setDisable(false);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);
        canInteractWithVertices = false;
    }
    private static int getResourceAmount(Player player, String resourceType)
    {
        switch (resourceType)
        {
            case "TALENT": return player.talentCount;
            case "CAPITAL": return player.capitalCount;
            case "CLOUD": return player.cloudCount;
            case "PATENT": return player.patentCount;
            case "DATA": return player.dataCount;
            default: return 0;
        }
    }
    private static HBox createShopRow(String name, int price, int currentAmount, String resourceType, Player player, VBox dialog, Pane overlay, Text capitalText)
    {
        HBox row = new HBox(15);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 5;");
        
        Text nameText = new Text(name);
        nameText.setFill(Color.WHITE);
        nameText.setFont(Font.font(14));
        
        Text priceText = new Text(price + " 💰");
        priceText.setFill(Color.web(CONSTS.COLOR_GREEN));
        priceText.setFont(Font.font(14));
        
        Text amountText = new Text("Owned: " + currentAmount);
        amountText.setFill(Color.LIGHTGRAY);
        amountText.setFont(Font.font(12));
        
        Button buyBtn = new Button("BUY");
        buyBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 5 15; -fx-background-radius: 5;");
        buyBtn.setCursor(javafx.scene.Cursor.HAND);
        buyBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(buyBtn, 1.2, 1000).play());
        buyBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(buyBtn, 1.0, 1000).play());
        
        buyBtn.setOnAction(e -> {
            if (player.capitalCount >= price)
            {
                player.capitalCount -= price;
                
                switch (resourceType)
                {
                    case "TALENT":
                        player.talentCount += 1;
                        talentwasBought = true;
                        talentNotBoughtForNRounds = 0;
                        break;
                    case "CLOUD":
                        player.cloudCount += 1;
                        cloudwasBought = true;
                        cloudNotBoughtForNRounds = 0;
                        break;
                    case "PATENT":
                        player.patentCount += 1;
                        patentwasBought = true;
                        patentNotBoughtForNRounds = 0;
                        break;
                    case "DATA":
                        player.dataCount += 1;
                        datawasBought = true;
                        dataNotBoughtForNRounds = 0;
                        break;
                }
                
                capitalText.setText("💰: " + player.capitalCount);
                amountText.setText("OWNED: " + getResourceAmount(player, resourceType));
                
                Text successMsg = new Text("Purchased " + name + "!");
                successMsg.setFill(Color.web(CONSTS.COLOR_GREEN));
                successMsg.setFont(Font.font(12));
                dialog.getChildren().add(successMsg);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
                pause.setOnFinished(ev -> dialog.getChildren().remove(successMsg));
                pause.play();
            }
            else
            {
                Text errorMsg = new Text("GET YO BROKE AHH OUTTA HERE!");
                errorMsg.setFill(Color.web(CONSTS.COLOR_RED));
                errorMsg.setFont(Font.font(12));
                dialog.getChildren().add(errorMsg);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(errorMsg));
                pause.play();
            }
        });
        
        row.getChildren().addAll(nameText, priceText, amountText, buyBtn);
        return row;
    }
    public static void createDiceStatusDisplay(Pane root)
    {
        diceStatusText = new Text("⚄: WAITING FOR PLAYER " + (currentPlayerIdx + 1) + " TO ROLL...");
        diceStatusText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        diceStatusText.setFill(Color.WHITE);
        diceStatusText.setStyle("-fx-font-weight: bold;");
        diceStatusText.setLayoutX((CONSTS.WINDOW_WIDTH - 300) / 2);
        diceStatusText.setLayoutY(CONSTS.WINDOW_HEIGHT - 110);
        root.getChildren().add(diceStatusText);
    }
    public static void showTaxDialog(List<Player> players, int playerIndex)
    {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = rootPane.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_RED + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(400);
        dialog.setPrefHeight(550);
        
        Text titleText = new Text("💰 BRING ME THE MONEY BOI! 💰");
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily(), 24));
        titleText.setFill(Color.web(CONSTS.COLOR_RED));
        titleText.setStyle("-fx-font-weight: bold;");
        
        Text infoText = new Text("PLAYER " + (currentTaxPayer.idx + 1) + " - PAY TAX BRO");
        infoText.setFill(Color.WHITE);
        infoText.setFont(Font.font(14));
        
        Text totalText = new Text("TOTAL RESOURCES: " + (currentTaxPayer.talentCount + currentTaxPayer.capitalCount + currentTaxPayer.cloudCount + currentTaxPayer.patentCount + currentTaxPayer.dataCount));
        totalText.setFill(Color.WHITE);
        totalText.setFont(Font.font(14));
        
        Text targetText = new Text("YOU MUST PAY: " + taxTarget + " RESOURCES");
        targetText.setFill(Color.web(CONSTS.COLOR_RED));
        targetText.setFont(Font.font(16));
        targetText.setStyle("-fx-font-weight: bold;");
        
        Text paidText = new Text("PAID: 0 / " + taxTarget);
        paidText.setFill(Color.web(CONSTS.COLOR_GREEN));
        paidText.setFont(Font.font(14));
        paidText.setStyle("-fx-font-weight: bold;");
        
        VBox resourceBox = new VBox(8);
        resourceBox.setAlignment(javafx.geometry.Pos.CENTER);
        resourceBox.setStyle("-fx-padding: 10; -fx-background-color: #222; -fx-background-radius: 10;");
        
        String resources[] = {"TALENT", "CAPITAL", "CLOUD", "PATENT", "DATA"};
        Map<String, Integer> maxValues = new HashMap<String, Integer>();
        maxValues.put("TALENT", currentTaxPayer.talentCount);
        maxValues.put("CAPITAL", currentTaxPayer.capitalCount);
        maxValues.put("CLOUD", currentTaxPayer.cloudCount);
        maxValues.put("PATENT", currentTaxPayer.patentCount);
        maxValues.put("DATA", currentTaxPayer.dataCount);
        
        Map<String, Spinner<Integer>> spinners = new HashMap<>();
        
        for (String resource : resources)
        {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Text label = new Text(resource + ":");
            label.setFill(Color.WHITE);
            label.setFont(Font.font(14));
            
            Spinner<Integer> spinner = new Spinner<>(0, maxValues.get(resource), 0);
            spinner.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
            spinner.setPrefWidth(80);
            spinners.put(resource, spinner);
            
            spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                int totalSelected = 0;
                for (String res : resources)
                    totalSelected += spinners.get(res).getValue();

                paidText.setText("PAID: " + totalSelected + " / " + taxTarget);
            });
            
            Text ownedText = new Text("/ " + maxValues.get(resource));
            ownedText.setFill(Color.LIGHTGRAY);
            ownedText.setFont(Font.font(12));
            
            row.getChildren().addAll(label, spinner, ownedText);
            resourceBox.getChildren().add(row);
        }
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button payBtn = new Button("PAY TAX");
        payBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        payBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(payBtn, 1.2, 1000).play());
        payBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(payBtn, 1.0, 1000).play());
        payBtn.setCursor(javafx.scene.Cursor.HAND);
        payBtn.setOnAction(e -> {
            int totalSelected = 0;
            for (String resource : resources)
            {
                int value = spinners.get(resource).getValue();
                totalSelected += value;
                taxSelection.put(resource, value);
            }
            
            if (totalSelected != taxTarget)
            {
                Text error = new Text("YOU MUST PAY EXACTLY " + taxTarget + " RESOURCES");
                error.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(error);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(error));
                pause.play();
                return;
            }
            
            for (String resource : resources)
            {
                int amount = taxSelection.get(resource);
                switch (resource)
                {
                    case "TALENT": currentTaxPayer.talentCount -= amount; break;
                    case "CAPITAL": currentTaxPayer.capitalCount -= amount; break;
                    case "CLOUD": currentTaxPayer.cloudCount -= amount; break;
                    case "PATENT": currentTaxPayer.patentCount -= amount; break;
                    case "DATA": currentTaxPayer.dataCount -= amount; break;
                }
            }
            
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
            
            Pane successOverlay = new Pane();
            successOverlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
            VBox successDialog = new VBox(15);
            successDialog.setAlignment(javafx.geometry.Pos.CENTER);
            successDialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25;");
            successDialog.setPrefWidth(300);
            successDialog.setPrefHeight(150);
            Text successText = new Text("💰 TAX PAID! 💰");
            successText.setFill(Color.web(CONSTS.COLOR_GREEN));
            successText.setFont(Font.font(16));
            successText.setStyle("-fx-font-weight: bold;");
            Button okBtn = new Button("OK");
            okBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
            okBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.2, 1000).play());
            okBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(okBtn, 1.0, 1000).play());
            okBtn.setCursor(javafx.scene.Cursor.HAND);
            okBtn.setOnAction(ev -> {
                rootPane.getChildren().remove(successOverlay);
                showNextTaxPlayer(players, playerIndex + 1);
            });
            successDialog.getChildren().addAll(successText, okBtn);
            successDialog.setLayoutX((CONSTS.WINDOW_WIDTH - successDialog.getPrefWidth()) / 2);
            successDialog.setLayoutY((CONSTS.WINDOW_HEIGHT - successDialog.getPrefHeight()) / 2);
            successOverlay.getChildren().add(successDialog);
            rootPane.getChildren().add(successOverlay);
            updatePlayerInfo();
        });
        
        buttonBox.getChildren().addAll(payBtn);
        
        dialog.getChildren().addAll(titleText, infoText, totalText, targetText, paidText, resourceBox, buttonBox);
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        rootPane.getChildren().add(overlay);
    }
    public static void rollDice()
    {
        currentDieNum = (int)(11 * Math.random() + 2); // [2, 13)
        diceStatusText.setText("⚄: " + currentDieNum);
        
        if (currentDieNum == 7)
        {
            Sector taxSector = null;
            for (Sector s : SECTORS)
            {
                if (s.hasTaxAgent)
                {
                    taxSector = s;
                    break;
                }
            }
            
            if (taxSector != null)
            {
                int vertexIndices[] = HELPERS.getVertexIdxFromSectorIdx(taxSector.idx);
                List<Player> playersOnSector = new ArrayList<Player>();
                
                for (int vIdx : vertexIndices)
                {
                    int row = vIdx / (CONSTS.MAX_WIDTH + 1);
                    int col = vIdx % (CONSTS.MAX_WIDTH + 1);
                    Vertex v = VERTICES[row][col];
                    if (v.owner != null && !playersOnSector.contains(v.owner))
                        playersOnSector.add(v.owner);
                }
                
                boolean hasTaxToPay = false;
                for (Player p : playersOnSector)
                {
                    int totalResources = p.talentCount + p.capitalCount + p.cloudCount + p.patentCount + p.dataCount;
                    int threshold = p.cardLimit;
                    
                    if (totalResources >= threshold)
                    {
                        hasTaxToPay = true;
                        break;
                    }
                }
                
                if (hasTaxToPay)
                {
                    currentTaxPayer = null;
                    taxTarget = 0;
                    currentTaxPaid = 0;
                    taxSelection.clear();
                    taxSelection.put("TALENT", 0);
                    taxSelection.put("CAPITAL", 0);
                    taxSelection.put("CLOUD", 0);
                    taxSelection.put("PATENT", 0);
                    taxSelection.put("DATA", 0);
                    
                    isPayingTax = true;
                    canInteractWithVertices = false;
                    diceBtn.setDisable(true);
                    shopBtn.setDisable(true);
                    tradeBtn.setDisable(true);
                    nextBtn.setDisable(true);
                    
                    showNextTaxPlayer(playersOnSector, 0);
                    return;
                }
            }
            
            diceStatusText.setText("⚄: 7 - SELECT A SECTOR FOR TAX AGENT");
            startSectorSelection();
            return;
        }
        
        processDiceRoll();
    }
    public static void showNextTaxPlayer(List<Player> players, int index)
    {
        if (index >= players.size())
        {
            isPayingTax = false;
            isSelectingSector = false;
            canInteractWithVertices = true;
            
            diceBtn.setDisable(true);
            shopBtn.setDisable(true);
            tradeBtn.setDisable(true);
            nextBtn.setDisable(true);
            
            diceStatusText.setText("⚄: 7 - SELECT A SECTOR FOR TAX AGENT");
            
            startSectorSelection();
            return;
        }
        
        Player p = players.get(index);
        int totalResources = p.talentCount + p.capitalCount + p.cloudCount + p.patentCount + p.dataCount;
        int threshold = p.cardLimit;
        
        if (totalResources >= threshold)
        {
            currentTaxPayer = p;
            taxTarget = (int)(totalResources / 2);
            currentTaxPaid = 0;
            taxSelection.clear();
            taxSelection.put("TALENT", 0);
            taxSelection.put("CAPITAL", 0);
            taxSelection.put("CLOUD", 0);
            taxSelection.put("PATENT", 0);
            taxSelection.put("DATA", 0);
            
            showTaxDialog(players, index);
        }
        else
            showNextTaxPlayer(players, index + 1);
    }

    public static void processDiceRoll()
    {
        int dieNum = currentDieNum;
        
        for (Sector s : SECTORS)
        {
            if (s.dieNum == dieNum && !(s.hasTaxAgent))
            {
                for (int vIdx : HELPERS.getVertexIdxFromSectorIdx(s.idx))
                {
                    Vertex vert = null;
                    boolean __shouldBreak = false;
                    for (int i = 0; i != VERTICES.length; i++)
                    {
                        for (int j = 0; j != VERTICES[i].length; j++)
                        {
                            if (VERTICES[i][j].idx == vIdx)
                            {
                                vert = VERTICES[i][j];
                                __shouldBreak = true;
                                break;
                            }
                        }
                        if (__shouldBreak)
                            break;
                    }
                    Player __owner = vert.owner;
                    if (__owner == null)
                        continue;
                    switch (CONSTS.SECTOR_REWARDS_HM.get(s.type))
                    {
                        case CONSTS.REWARD_TYPE_AI:
                            __owner.talentCount += 1;
                            break;
                        case CONSTS.REWARD_TYPE_CLOUD:
                            __owner.cloudCount += 1;
                            break;
                        case CONSTS.REWARD_TYPE_DATA:
                            __owner.dataCount += 1;
                            break;
                        case CONSTS.REWARD_TYPE_FINTECH:
                            __owner.capitalCount += 1;
                            break;
                        case CONSTS.REWARD_TYPE_IP:
                            __owner.patentCount += 1;
                            break;
                        case CONSTS.REWARD_TYPE_REGU:
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        
        updatePlayerInfo();
        updateButtonsAfterRoll();
    }

    public static void openShop(Player player)
    {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = rootPane.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(350);
        dialog.setPrefHeight(450);
        
        Text titleText = new Text("SHOP");
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        titleText.setFill(Color.WHITE);
        titleText.setStyle("-fx-font-weight: bold;");
        
        Text capitalText = new Text("💰: " + player.capitalCount);
        capitalText.setFill(Color.web(CONSTS.COLOR_GREEN));
        capitalText.setFont(Font.font(16));
        capitalText.setStyle("-fx-font-weight: bold;");
        
        VBox pricesBox = new VBox(10);
        pricesBox.setAlignment(javafx.geometry.Pos.CENTER);
        pricesBox.setStyle("-fx-padding: 10; -fx-background-color: #222; -fx-background-radius: 10;");
        
        int __talentPrice = (player.role != CONSTS.PLAYER_ROLE_CEO) ? talentPrice : talentPrice / 4 * 3;
        int __cloudPrice  = (player.role != CONSTS.PLAYER_ROLE_CEO) ? cloudPrice  : cloudPrice  / 4 * 3;
        int __patentPrice = (player.role != CONSTS.PLAYER_ROLE_CEO) ? patentPrice : patentPrice / 4 * 3;
        int __dataPrice   = (player.role != CONSTS.PLAYER_ROLE_CEO) ? dataPrice   : dataPrice   / 4 * 3;
        HBox talentRow = createShopRow("🎓", __talentPrice, player.talentCount, "TALENT", player, dialog, overlay, capitalText);
        HBox cloudRow = createShopRow("☁️", __cloudPrice, player.cloudCount, "CLOUD", player, dialog, overlay, capitalText);
        HBox patentRow = createShopRow("📜", __patentPrice, player.patentCount, "PATENT", player, dialog, overlay, capitalText);
        HBox dataRow = createShopRow("📊", __dataPrice, player.dataCount, "DATA", player, dialog, overlay, capitalText);
        
        pricesBox.getChildren().addAll(talentRow, cloudRow, patentRow, dataRow);
        
        Button closeBtn = new Button("CLOSE");
        closeBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
        closeBtn.setCursor(javafx.scene.Cursor.HAND);
        closeBtn.setOnMouseEntered(e -> HELPERS.createCustomScaleAnimation(closeBtn, 1.2, 800).play());
        closeBtn.setOnMouseExited(e -> HELPERS.createCustomScaleAnimation(closeBtn, 1.0, 800).play());
        closeBtn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
            updatePlayerInfo();
        });
        
        dialog.getChildren().addAll(titleText, capitalText, pricesBox, closeBtn);
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        rootPane.getChildren().add(overlay);
    }

    public static void openTrade()
    {
        Player currentPlayer = getCurrentPlayer();
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = rootPane.snapshot(params, null);
        
        ImageView backgroundImage = new ImageView(snapshot);
        GaussianBlur blur = new GaussianBlur(10);
        backgroundImage.setEffect(blur);
        
        Pane overlay = new Pane();
        overlay.setPrefSize(CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        overlay.getChildren().add(backgroundImage);
        
        VBox dialog = new VBox(15);
        dialog.setAlignment(javafx.geometry.Pos.CENTER);
        dialog.setStyle("-fx-background-color: #111111; -fx-background-radius: 20; -fx-padding: 25; -fx-border-color: " + CONSTS.COLOR_GREEN + "; -fx-border-width: 2; -fx-border-radius: 20;");
        dialog.setPrefWidth(500);
        dialog.setPrefHeight(600);
        
        Text titleText = new Text("PLAYER " + (currentPlayer.idx + 1) + " - SET UP OFFER");
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT.getFamily()));
        titleText.setFill(Color.WHITE);
        titleText.setStyle("-fx-font-weight: bold;");
        
        VBox playerBox = new VBox(10);
        playerBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text playerLabel = new Text("SELECT TRADE PARTNER:");
        playerLabel.setFill(Color.WHITE);
        playerLabel.setFont(Font.font(14));
        
        ToggleGroup playerGroup = new ToggleGroup();
        VBox playerRadioBox = new VBox(5);
        playerRadioBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        for (int i = 0; i<playerCount; i++)
        {
            if (i!=currentPlayer.idx)
            {
                RadioButton playerBtn = new RadioButton("PLAYER " + (i + 1));
                playerBtn.setToggleGroup(playerGroup);
                playerBtn.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                playerBtn.setCursor(javafx.scene.Cursor.HAND);
                playerRadioBox.getChildren().add(playerBtn);
            }
        }
        playerBox.getChildren().addAll(playerLabel, playerRadioBox);
        
        VBox giveBox = new VBox(5);
        giveBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text giveLabel = new Text("GIVE:");
        giveLabel.setFill(Color.web(CONSTS.COLOR_GREEN));
        giveLabel.setFont(Font.font(14));
        giveLabel.setStyle("-fx-font-weight: bold;");
        
        GridPane giveGrid = new GridPane();
        giveGrid.setAlignment(javafx.geometry.Pos.CENTER);
        giveGrid.setHgap(10);
        giveGrid.setVgap(5);
        
        String resources[] = {"TALENT", "CAPITAL", "CLOUD", "PATENT", "DATA"};
        Spinner<Integer>[] giveSpinners = new Spinner[5];
        
        for (int i = 0; i<resources.length; i++)
        {
            Text resourceText = new Text(resources[i]);
            resourceText.setFill(Color.WHITE);
            resourceText.setFont(Font.font(12));
            
            Spinner<Integer> spinner = new Spinner<Integer>(0, 99, 0);
            spinner.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
            spinner.setEditable(true);
            giveSpinners[i] = spinner;
            
            giveGrid.add(resourceText, 0, i);
            giveGrid.add(spinner, 1, i);
        }
        giveBox.getChildren().addAll(giveLabel, giveGrid);
        
        VBox receiveBox = new VBox(5);
        receiveBox.setAlignment(javafx.geometry.Pos.CENTER);
        Text receiveLabel = new Text("GET:");
        receiveLabel.setFill(Color.web(CONSTS.COLOR_GREEN));
        receiveLabel.setFont(Font.font(14));
        receiveLabel.setStyle("-fx-font-weight: bold;");
        
        GridPane receiveGrid = new GridPane();
        receiveGrid.setAlignment(javafx.geometry.Pos.CENTER);
        receiveGrid.setHgap(10);
        receiveGrid.setVgap(5);
        
        Spinner<Integer> receiveSpinners[] = new Spinner[5];
        
        for (int i = 0; i<resources.length; i++)
        {
            Text resourceText = new Text(resources[i]);
            resourceText.setFill(Color.WHITE);
            resourceText.setFont(Font.font(12));
            
            Spinner<Integer> spinner = new Spinner<Integer>(0, 99, 0);
            spinner.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
            spinner.setEditable(true);
            receiveSpinners[i] = spinner;
            
            receiveGrid.add(resourceText, 0, i);
            receiveGrid.add(spinner, 1, i);
        }
        receiveBox.getChildren().addAll(receiveLabel, receiveGrid);
        
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button askBtn = new Button("OFFER");
        askBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_GREEN + "; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        askBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(askBtn, 1.2, 1000).play());
        askBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(askBtn, 1.0, 1000).play());
        askBtn.setCursor(javafx.scene.Cursor.HAND);
        
        Button cancelBtn = new Button("CANCEL");
        cancelBtn.setStyle("-fx-background-color: " + CONSTS.COLOR_RED + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        cancelBtn.setOnMouseEntered(ev -> HELPERS.createCustomScaleAnimation(cancelBtn, 1.2, 1000).play());
        cancelBtn.setOnMouseExited(ev -> HELPERS.createCustomScaleAnimation(cancelBtn, 1.0, 1000).play());
        cancelBtn.setCursor(javafx.scene.Cursor.HAND);
        
        askBtn.setOnAction(e -> {
            RadioButton selected = (RadioButton) playerGroup.getSelectedToggle();
            if (selected == null)
            {
                Text error = new Text("SELECT A TRADE PARTNER");
                error.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(error);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(error));
                pause.play();
                return;
            }
            
            int selectedIdx = Integer.parseInt(selected.getText().split(" ")[1]) - 1;
            selectedTradePlayer = PLAYERS[selectedIdx];
            
            currentOffer.clear();
            for (int i = 0; i < resources.length; i++)
            {
                currentOffer.put(resources[i] + "_GIVE", giveSpinners[i].getValue());
                currentOffer.put(resources[i] + "_get", receiveSpinners[i].getValue());
            }
            
            boolean hasEnough = true;
            if (giveSpinners[0].getValue() > currentPlayer.talentCount) hasEnough = false;
            if (giveSpinners[1].getValue() > currentPlayer.capitalCount) hasEnough = false;
            if (giveSpinners[2].getValue() > currentPlayer.cloudCount) hasEnough = false;
            if (giveSpinners[3].getValue() > currentPlayer.patentCount) hasEnough = false;
            if (giveSpinners[4].getValue() > currentPlayer.dataCount) hasEnough = false;
            
            if (!hasEnough)
            {
                Text error = new Text("GET YO BROKE AHH OUTTA HERE");
                error.setFill(Color.web(CONSTS.COLOR_RED));
                dialog.getChildren().add(error);
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> dialog.getChildren().remove(error));
                pause.play();
                return;
            }
            
            rootPane.getChildren().remove(overlay);
            showTradeOffer(selectedTradePlayer, currentPlayer);
        });
        
        cancelBtn.setOnAction(e -> {
            rootPane.getChildren().remove(overlay);
            rootPane.setEffect(null);
        });
        
        buttonBox.getChildren().addAll(askBtn, cancelBtn);
        
        dialog.getChildren().addAll(titleText, playerBox, giveBox, receiveBox, buttonBox);
        dialog.setLayoutX((CONSTS.WINDOW_WIDTH - dialog.getPrefWidth()) / 2);
        dialog.setLayoutY((CONSTS.WINDOW_HEIGHT - dialog.getPrefHeight()) / 2);
        
        overlay.getChildren().add(dialog);
        rootPane.getChildren().add(overlay);
    }

    public static void nextTurn()
    {
        // 1 NOBAT
        if (atEnd)
        {
            if (!talentwasBought)
                talentNotBoughtForNRounds += 1;
            else
            {
                talentPrice = Math.min(6, talentPrice + 1);
                talentNotBoughtForNRounds = 0;
            }
            if (!cloudwasBought)
                cloudNotBoughtForNRounds += 1;
            else
            {
                cloudPrice = Math.min(6, cloudPrice + 1);
                cloudNotBoughtForNRounds = 0;
            }
            if (!patentwasBought)
                patentNotBoughtForNRounds += 1;
            else
            {
                patentPrice = Math.min(6, patentPrice + 1);
                patentNotBoughtForNRounds = 0;
            }
            if (!datawasBought)
                dataNotBoughtForNRounds += 1;
            else
            {
                dataPrice = Math.min(6, dataPrice + 1);
                dataNotBoughtForNRounds = 0;
            }

            if (talentNotBoughtForNRounds >= 3)
            {
                talentPrice = Math.max(2, talentPrice - 1);
                talentNotBoughtForNRounds = 0;
            }
            if (cloudNotBoughtForNRounds >= 3)
            {
                cloudPrice = Math.max(2, cloudPrice - 1);
                cloudNotBoughtForNRounds = 0;
            }
            if (patentNotBoughtForNRounds >= 3)
            {
                patentPrice = Math.max(2, patentPrice - 1);
                patentNotBoughtForNRounds = 0;
            }
            if (dataNotBoughtForNRounds >= 3)
            {
                dataPrice = Math.max(2, dataPrice - 1);
                dataNotBoughtForNRounds = 0;
            }
            talentwasBought = false;
            cloudwasBought = false;
            patentwasBought = false;
            datawasBought = false;

            atEnd = false;
            direction *= -1;
            currentPlayerIdx += direction;
        }
        else
        {
            int nextIdx = currentPlayerIdx + direction;
            
            if (nextIdx < 0 || nextIdx >= playerCount)
                atEnd = true;
            else
                currentPlayerIdx = nextIdx;
        }

        System.out.println("Next player: " + (currentPlayerIdx + 1));
        diceStatusText.setText("⚄: WAITING FOR PLAYER " + (currentPlayerIdx + 1) + " TO ROLL...");
        updatePlayerInfo();
        updateButtonsAfterNext();
    }
}