import javafx.scene.*;
import javafx.util.*;
import javafx.animation.*;
import javafx.scene.layout.*;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.*;

class HELPERS
{
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
            int row = i / CONSTS.MAX_HEIGHT;
            int col = i % CONSTS.MAX_WIDTH;
            
            double x = CONSTS.OFFSET_X + col * CONSTS.CELL_SIZE + (CONSTS.CELL_SIZE - CONSTS.SECTOR_SIZE) / 2;
            double y = CONSTS.OFFSET_Y + row * CONSTS.CELL_SIZE + (CONSTS.CELL_SIZE - CONSTS.SECTOR_SIZE) / 2;
            
            StackPane sectorPane = new StackPane();
            sectorPane.setLayoutX(x);
            sectorPane.setLayoutY(y);
            sectorPane.setPrefSize(CONSTS.SECTOR_SIZE, CONSTS.SECTOR_SIZE);
            
            Rectangle rect = new Rectangle(CONSTS.SECTOR_SIZE, CONSTS.SECTOR_SIZE);
            rect.setFill(Color.web(CONSTS.SECTOR_COLORS_HM.get(sector.type)));
            rect.setStroke(Color.BLACK);
            rect.setStrokeWidth(2);
            
            VBox vbox = new javafx.scene.layout.VBox(5);
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            
            Text typeText = new Text(sector.type);
            typeText.setFill(Color.WHITE);
            typeText.setFont(CONSTS.CUSTOM_FONT != null ? CONSTS.CUSTOM_FONT : Font.font(10));
            typeText.setStyle("-fx-font-weight: bold;");
            
            Text dieText = new Text(String.valueOf(sector.dieNum));
            dieText.setFill(Color.WHITE);
            dieText.setFont(CONSTS.CUSTOM_FONT != null ? CONSTS.CUSTOM_FONT : Font.font(14));
            dieText.setStyle("-fx-font-weight: bold;");
            
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

                if (vertex.owner == null || vertex.owner == Main.getCurrentPlayer())
                {
                    stackPane.setOnMouseEntered(event -> {
                        Timeline timeline = HELPERS.createCustomScaleAnimation(stackPane, 1.4, 800);
                        timeline.play();
                    });
    
                    stackPane.setOnMouseExited(event -> {
                        Timeline timeline = HELPERS.createCustomScaleAnimation(stackPane, 1.0, 800);
                        timeline.play();
                    });
                    stackPane.setOnMouseClicked(event -> {
                        Player currentPlayer = Main.getCurrentPlayer();
                        
                        if (vertex.type != CONSTS.VERTEX_TYPE_UNICORN)
                            showPurchaseMenu(root, vertex, currentPlayer);
                        else
                            showVertexInfo(root, vertex, currentPlayer);
                    });
                    stackPane.setCursor(javafx.scene.Cursor.HAND);
                }
                
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
    public static void showPurchaseMenu(Pane root, Vertex vertex, Player player)
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
        System.out.println("Vertex " + vertex.idx + " owned by Player " + vertex.owner.idx + " | Type: " + vertex.type);
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
        titleText.setFont(Font.font(CONSTS.CUSTOM_FONT != null ? CONSTS.CUSTOM_FONT.getFamily() : "Arial", 20));
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
                errorMsg.setFill(Color.RED);
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
        // TODO: Add PARTNERSHIP case when costs are defined
        
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
            player.score += 2;
        else if (type.equals("PARTNERSHIP"))
        {
            // TODO: Add PARTNERSHIP specific logic
        }
        
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

class Sector
{
    public int idx;
    public String type;
    public int dieNum;

    public Sector(int i)
    {
        idx = i;

        String types[] = {CONSTS.SECTOR_TYPE_AI, CONSTS.SECTOR_TYPE_FINTECH, CONSTS.SECTOR_TYPE_CLOUD, CONSTS.SECTOR_TYPE_IP, CONSTS.SECTOR_TYPE_DATA, CONSTS.SECTOR_TYPE_REGU};
        type = types[(int)(Math.random() * types.length)];

        dieNum = (int)(11 * Math.random() + 2);
    }
}

public class Main extends Application
{
    static Player PLAYERS[]      = new Player[CONSTS.MAX_PLAYER_COUNT];
    static int currentPlayerIdx  = 0;
    static int EDGES_FROM_TO[][] = new int[CONSTS.MAX_HEIGHT][CONSTS.MAX_WIDTH];
    static Sector SECTORS[]      = new Sector[CONSTS.MAX_HEIGHT * CONSTS.MAX_HEIGHT];
    static Vertex VERTICES[][]   = new Vertex[CONSTS.MAX_HEIGHT+1][CONSTS.MAX_WIDTH+1];

    static int playerCount;
    static Button diceBtn;
    static Button shopBtn;
    static Button tradeBtn;
    static Button nextBtn;
    static HBox buttonBar;

    // SHOP RELATED
    static int talentPrice  = 4;
    static int capitalPrice = 4;
    static int cloudPrice   = 4;
    static int patentPrice  = 4;
    static int dataPrice    = 4;

    static boolean talentwasBought  = false;
    static boolean capitalwasBought = false;
    static boolean cloudwasBought   = false;
    static boolean patentwasBought  = false;
    static boolean datawasBought    = false;

    static int talentNotBoughtForNRounds  = 0;
    static int capitalNotBoughtForNRounds = 0;
    static int cloudNotBoughtForNRounds   = 0;
    static int patentNotBoughtForNRounds  = 0;
    static int dataNotBoughtForNRounds    = 0;
    // END SHOP RELATED

    @Override
    public void start(Stage primaryStage)
    {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: #000000;");

        HELPERS.drawSectors(root, SECTORS);
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
        shopBtn.setOnAction(e -> openShop());
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
        
        Scene scene = new Scene(root, CONSTS.WINDOW_WIDTH, CONSTS.WINDOW_HEIGHT);
        primaryStage.setTitle("MONOPOLY LOOKING AHH GAME");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String args[])
    {
        // INIT PLAYERS
        // TODO
        PLAYERS[0] = new Player(0);
        playerCount = 1;
        // END INIT PLAYERS
        
        // INIT EDGES
        for (int i = 0; i<CONSTS.MAX_HEIGHT; i++)
            for (int j = 0; j<CONSTS.MAX_WIDTH; j++)
                EDGES_FROM_TO[i][j] = 0;
        // END INIT EDGES

        // INIT SECTORS
        for (int i = 0; i<CONSTS.MAX_HEIGHT * CONSTS.MAX_HEIGHT; i++)
            SECTORS[i] = new Sector(i);
        // END INIT SECTORS

        // INIT VERTICES
        {
            int count = 0;
            for (int i = 0; i<CONSTS.MAX_HEIGHT+1; i++)
                for (int j = 0; j<CONSTS.MAX_WIDTH+1; j++)
                    VERTICES[i][j] = new Vertex(count++);
        }
        // END INIT VERTICES

        launch(args);
    }

    // APIs/others
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
    }

    public static void updateButtonsAfterNext()
    {
        diceBtn.setDisable(false);
        shopBtn.setDisable(true);
        tradeBtn.setDisable(true);
        nextBtn.setDisable(true);
    }
    public static void rollDice()
    {
        int dieNum = (int)(11 * Math.random() + 2);
        if (dieNum != 7)
        {
            for (Sector s : SECTORS)
            {
                if (s.dieNum == dieNum)
                {
                    for (int vIdx : HELPERS.getVertexIdxFromSectorIdx(s.idx))
                    {
                        Vertex vert = null;
                        boolean __shouldBreak = false;
                        for (int i = 0; i!=VERTICES.length; i++)
                        {
                            for (int j = 0; j!=VERTICES[i].length; j++)
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
        }
        else
        {
            // TODO when = 7 (regulatory)
        }
        updateButtonsAfterRoll();
    }

    public static void openShop()
    {
        // TODO: Implement shop logic
        System.out.println("Shop opened!");
    }

    public static void openTrade()
    {
        // TODO: Implement trade logic
        System.out.println("Trade opened!");
    }

    public static void nextTurn()
    {
        // TODO: Implement next player logic
        currentPlayerIdx = (currentPlayerIdx + 1) % playerCount;
        System.out.println("Next player: " + currentPlayerIdx);
        updateButtonsAfterNext();
    }
}