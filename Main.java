import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

class CONSTS
{
    public final static int MAP_HEIGHT                 = 5;
    public final static int MAP_WIDTH                  = 5;
 
    public final static String SECTOR_TYPE_AI          = "AI";
    public final static String SECTOR_TYPE_FINTECH     = "FINTECH";
    public final static String SECTOR_TYPE_CLOUD       = "CLOUD";
    public final static String SECTOR_TYPE_DATA_ANAL   = "DATA ANALS";
    public final static String SECTOR_TYPE_PATENT      = "PATENT";
    
    public final static int SECTOR_1D_DIM              = 100;
    
    public final static int WIN_SCORE                  = 10;

    public final static String COLOR_TEXT              = "#eeeeee";
    public final static String COLOR_BG                = "#000000";
}

class HELPERS
{
    public static double getDist(Point p, Point q)
    {
        return Math.sqrt(Math.pow(q.x-p.x, 2) + Math.pow(q.y-p.y, 2));
    }

    public static Color getColorFromType(String t)
    {
        switch (t)
        {
            case CONSTS.SECTOR_TYPE_AI:
                return Color.web("#443199");
            case CONSTS.SECTOR_TYPE_FINTECH:
                return Color.web("#111844");
            case CONSTS.SECTOR_TYPE_CLOUD:
                return Color.web("#C13383");
            case CONSTS.SECTOR_TYPE_DATA_ANAL:
                return Color.web("#E05454");
            case CONSTS.SECTOR_TYPE_PATENT:
                return Color.web("#792CA2");
            default:
                return Color.GRAY;
        }
    }
}

class Point
{
    public int x;
    public int y;

    public Point(int x, int y)
    {
        this.x = x;
        this.y = y;
    }
}

class Player
{
    public int shownIndex; // 1, 2, 3, 4
    public int score;
}

class Company
{
    public ArrayList<Player> owners;
    public Point pos;

    public Company(Player o1, Player o2, int x, int y)
    {
        owners = new ArrayList<Player>();
        owners.add(o1);
        if (o2 != null)
            owners.add(o2);

        pos = new Point(x, y);
    }
}
class MVP extends Company
{
    public MVP(Player o1, Player o2, int x, int y)
    {
        super(o1, o2, x, y);
    }
}
class Unicorn extends Company
{
    public Unicorn(Player o1, Player o2, int x, int y)
    {
        super(o1, o2, x, y);
    }
}
class Partnership extends Company
{
    public Partnership(Player o1, Player o2, int x, int y)
    {
        super(o1, o2, x, y);
    }
}

class Node
{
    public Company company;
    public Point pos;

    public Node(int x, int y)
    {
        pos = new Point(x, y);
    }
}
class Sector
{
    public Node nodes[] = new Node[4]; // {topLeft, topRight, bottomLeft, bottomRight}
    public String type; // use CONSTS.SECTOR_TYPE_X
    public int diceNumber; // 2-12
    public Point pos;

    public Sector(int x, int y)
    {
        for (int i = 0; i!=4; i++)
        {
            switch (i)
            {
                case 0:
                    nodes[i] = new Node(x, y);
                    break;
                case 1:
                    nodes[i] = new Node(x+CONSTS.SECTOR_1D_DIM, y);
                    break;
                case 2:
                    nodes[i] = new Node(x, y+CONSTS.SECTOR_1D_DIM);
                    break;
                case 3:
                    nodes[i] = new Node(x+CONSTS.SECTOR_1D_DIM, y+CONSTS.SECTOR_1D_DIM);
                    break;
            }
        }

        String __l[] = {CONSTS.SECTOR_TYPE_AI, CONSTS.SECTOR_TYPE_CLOUD, CONSTS.SECTOR_TYPE_DATA_ANAL, CONSTS.SECTOR_TYPE_FINTECH, CONSTS.SECTOR_TYPE_PATENT};
        type = __l[(int)(__l.length * Math.random())];

        diceNumber = (int)((11 * Math.random()) + 2); // [2, 13)

        pos = new Point(x, y);
    }    
}

public class Main extends Application {
    
    @Override
    public void start(Stage stage)
    {
        GridPane grid = new GridPane();
        grid.setHgap(CONSTS.MAP_HEIGHT);
        grid.setVgap(CONSTS.MAP_WIDTH);
        grid.setStyle("-fx-padding: 40px; -fx-background-color: " + CONSTS.COLOR_BG + ";");
        grid.setAlignment(Pos.CENTER);
        // INIT SECTORS
        Sector MAP[][] = new Sector[CONSTS.MAP_HEIGHT][CONSTS.MAP_WIDTH];
        for (int i = 0; i!=CONSTS.MAP_HEIGHT; i++)
            for (int j = 0; j!=CONSTS.MAP_WIDTH; j++)
                MAP[i][j] = new Sector(i*CONSTS.SECTOR_1D_DIM, j*CONSTS.SECTOR_1D_DIM);

        for (int i = 0; i!=CONSTS.MAP_HEIGHT; i++)
        {
            for (int j = 0; j!=CONSTS.MAP_WIDTH; j++)
            {
                Sector s = MAP[i][j];
                for (int nodeIdx = 0; nodeIdx!=4; nodeIdx++)
                {
                    Node __node = s.nodes[nodeIdx];
                    boolean found = false;
                    for (int u = 0; u!=CONSTS.MAP_HEIGHT && !found; u++)
                    {
                        for (int v = 0; v!=CONSTS.MAP_WIDTH && !found; v++)
                        {
                            if (u==i && v==j) continue;
                            
                            Sector other = MAP[u][v];
                            
                            for (int otherIdx = 0; otherIdx!=4 && !found; otherIdx++)
                            {
                                Node otherNode = other.nodes[otherIdx];
                                
                                if (HELPERS.getDist(__node.pos, otherNode.pos) == 0)
                                {
                                    s.nodes[nodeIdx] = otherNode;
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        for (int i = 0; i!=CONSTS.MAP_HEIGHT; i++)
        {
            for (int j = 0; j!=CONSTS.MAP_WIDTH; j++)
            {
                Sector sector = MAP[i][j];
                
                StackPane square = new StackPane();
                square.setPrefSize(CONSTS.SECTOR_1D_DIM, CONSTS.SECTOR_1D_DIM);
                square.setStyle("-fx-border-color: white; -fx-border-width: 1px; -fx-border-radius: 5px;");
                
                Color bgColor = HELPERS.getColorFromType(sector.type);
                Rectangle bg = new Rectangle(CONSTS.SECTOR_1D_DIM, CONSTS.SECTOR_1D_DIM);
                bg.setFill(bgColor);
                bg.setArcWidth(10);
                bg.setArcHeight(10);
                
                Text typeText = new Text(sector.type);
                typeText.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: " + CONSTS.COLOR_TEXT + ";");
                
                Text diceText = new Text("" + sector.diceNumber);
                diceText.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-fill: " + CONSTS.COLOR_TEXT + ";");
                
                VBox labels = new VBox(5, diceText, typeText);
                labels.setStyle("-fx-alignment: center;");
                
                square.getChildren().addAll(bg, labels);
                
                grid.add(square, j, i);
            }
        }
        // END INIT SECTORS

        Scene scene = new Scene(grid, 700, 700);
        stage.setTitle("Monopoly FUM Knock-off");
        stage.setScene(scene);
        stage.show();
    }
    
    public static void main(String args[])
    {
        launch(args);
    }
}