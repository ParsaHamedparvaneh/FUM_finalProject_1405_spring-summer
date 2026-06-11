package packages;

import java.util.*;

public class GameLogic
{
    private List<Player> players;
    private Map<Integer, Player> playerMap;
    private Sector[][] map;
    private Node[][] nodes; // 6x6 nodes for 5x5 sectors
    private Edge[][] edges;
    private Edge[][] verticalEdges;
    private Market market;
    private int currentPlayerIndex;
    private int currentDiceSum;
    private Point auditorPosition;
    private boolean gameFinished;
    private Player winner;
    private List<String> eventLog;
    
    public GameLogic(List<String> playerNames)
    {
        players = new ArrayList<Player>();
        playerMap = new HashMap<Integer, Player>();
        for (int i = 0; i < playerNames.size(); i++)
        {
            Player p = new Player(i + 1, playerNames.get(i));
            players.add(p);
            playerMap.put(p.getId(), p);
        }
        
        initializeMap();
        initializeNodesAndEdges();
        
        market = new Market();
        currentPlayerIndex = 0;
        gameFinished = false;
        eventLog = new ArrayList<String>();
        auditorPosition = null;
    }
    
    private void initializeMap()
    {
        map = new Sector[Constants.MAP_HEIGHT][Constants.MAP_WIDTH];
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
                map[i][j] = new Sector(i * Constants.SECTOR_SIZE, j * Constants.SECTOR_SIZE);
    }
    
    private void initializeNodesAndEdges()
    {
        nodes = new Node[Constants.MAP_HEIGHT + 1][Constants.MAP_WIDTH + 1];
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++)
            for (int j = 0; j <= Constants.MAP_WIDTH; j++)
                nodes[i][j] = new Node(i * Constants.SECTOR_SIZE, j * Constants.SECTOR_SIZE);
        
        // Connect nodes ahh loop
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
            {
                Sector sector = map[i][j];
                nodes[i][j].addAdjacentSector(sector); // TopLeft
                nodes[i][j+1].addAdjacentSector(sector); // topRight
                nodes[i+1][j].addAdjacentSector(sector); // bl
                nodes[i+1][j+1].addAdjacentSector(sector); // br
            }
        }
        
        // Create edges ahh loop
        edges = new Edge[Constants.MAP_HEIGHT+1][Constants.MAP_WIDTH];
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++)
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
                edges[i][j] = new Edge(nodes[i][j], nodes[i][j+1]);
        
        verticalEdges = new Edge[Constants.MAP_HEIGHT][Constants.MAP_WIDTH+1];
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
            for (int j = 0; j <= Constants.MAP_WIDTH; j++)
                verticalEdges[i][j] = new Edge(nodes[i][j], nodes[i+1][j]);
    }
    
    public void startGame() { eventLog.add("Game started!"); performInitialPlacement(); }
    
    private void performInitialPlacement() {
        for (Player player : players)
        {
            Node availableNode = findAvailableNodeForPlacement();
            if (availableNode != null)
                placeMVP(player, availableNode, true);
        }
        
        for (int i = players.size() - 1; i >= 0; i--) {
            Player player = players.get(i);
            Node availableNode = findAvailableNodeForPlacement();
            if (availableNode != null)
            {
                placeMVP(player, availableNode, true);
                Edge adjacentEdge = findAdjacentFreeEdge(availableNode);
                if (adjacentEdge != null)
                    placePartnership(player, adjacentEdge, true);
            }
        }
        
        for (Player player : players)
        {
            for (Company company : player.getCompanies())
            {
                Node pos = company.getPosition();
                for (Sector sector : pos.getAdjacentSectors())
                {
                    String resource = sector.getResourceType();
                    if (resource != null)
                        player.getResources().addResource(resource, 1);
                }
            }
        }
        
        eventLog.add("Initial placement completed!");
    }
    
    private Node findAvailableNodeForPlacement()
    {
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++)
            {
                Node node = nodes[i][j];
                if (!node.hasCompany() && isValidPlacement(node))
                    return node;
            }
        }

        return null;
    }
    
    private boolean isValidPlacement(Node node)
    {
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++)
            {
                Node other = nodes[i][j];
                if (other.hasCompany())
                {
                    int dx = Math.abs(node.getPosition().x - other.getPosition().x);
                    int dy = Math.abs(node.getPosition().y - other.getPosition().y);
                    if (dx <= 2*Constants.SECTOR_SIZE && dy <= 2*Constants.SECTOR_SIZE)
                        return false;
                }
            }
        }
        return true;
    }
    
    private Edge findAdjacentFreeEdge(Node node)
    {
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
            {
                Edge edge = edges[i][j];
                if (!edge.hasPartnership() && (edge.getNode1().equals(node) || edge.getNode2().equals(node)))
                    return edge;
            }
        }
        
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++)
            {
                Edge edge = verticalEdges[i][j];
                if (!edge.hasPartnership() && (edge.getNode1().equals(node) || edge.getNode2().equals(node)))
                    return edge;
            }
        }
        return null;
    }
    
    public void rollDice()
    {
        Random rand = new Random();
        int dice1 = rand.nextInt(6) + 1;
        int dice2 = rand.nextInt(6) + 1;
        currentDiceSum = dice1 + dice2;
        eventLog.add("Player " + getCurrentPlayer().getName() + " rolled " + dice1 + " + " + dice2 + " = " + currentDiceSum);
        
        if (currentDiceSum == 7)
            handleRegulatoryCrisis();
        else
            produceResources();
    }
    
    private void produceResources()
    {
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
            {
                Sector sector = map[i][j];
                if (sector.canProduce() && sector.getDiceNumber() == currentDiceSum)
                {
                    String resource = sector.getResourceType();
                    if (resource != null)
                    {
                        Set<Player> producers = new HashSet<>();
                        Node[] sectorNodes = sector.getNodes();
                        for (Node node : sectorNodes)
                            if (node.hasCompany())
                                producers.add(node.getCompany().getOwner());
                        
                        for (Player player : producers)
                        {
                            int amount = 1;
                            for (Company company : player.getCompanies())
                            {
                                if (company.getPosition().equals(sectorNodes[0]) && company instanceof Unicorn)
                                {
                                    amount = Constants.SCORE_UNICORN;
                                    break;
                                }
                            }
                            player.getResources().addResource(resource, amount);
                            eventLog.add(player.getName() + " gained " + amount + " " + resource + " from sector at (" + i + "," + j + ")");
                        }
                    }
                }
            }
        }
    }
    
    private void handleRegulatoryCrisis()
    {
        eventLog.add("Regulatory Crisis! (Sum = 7)");
        
        for (Player player : players)
        {
            player.payTax();
            eventLog.add(player.getName() + " paid tax");
        }
        
        if (auditorPosition != null)
        {
            Sector oldSector = map[auditorPosition.x][auditorPosition.y];
            if (oldSector != null)
                oldSector.setAuditor(false);
        }
        
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
            {
                Sector sector = map[i][j];
                for (Node node : sector.getNodes())
                {
                    if (node.hasCompany())
                    {
                        auditorPosition = new Point(i, j);
                        sector.setAuditor(true);
                        eventLog.add("Auditor placed at sector (" + i + "," + j + ")");
                        return;
                    }
                }
            }
        }
    }
    
    public void buildMVP(Player player, Node position, Map<String, Integer> resources) throws InvalidPlacementException, InsufficientResourcesException {
        if (!isValidPlacement(position))
            throw new InvalidPlacementException("Invalid placement location");
        
        if (!player.getResources().hasEnough(resources))
            throw new InsufficientResourcesException("Not enough resources");
        
        player.getResources().deductResources(resources);
        MVP mvp = new MVP(player, position);
        player.getCompanies().add(mvp);
        position.setCompany(mvp);
        
        eventLog.add(player.getName() + " built MVP at (" + position.getPosition().x + "," + position.getPosition().y + ")");
    }
    
    public void upgradeToUnicorn(Player player, MVP mvp) throws InsufficientResourcesException
    {
        int cloudCost = Constants.UNICORN_UPGRADE_CLOUD;
        int dataCost = Constants.UNICORN_UPGRADE_DATA;
        
        if (player.getRole() != null && player.getRole().equals("CTO"))
            cloudCost -= Constants.CLOUD_DISCOUNT;
        
        if (player.getResources().getCount(Constants.RESOURCE_CLOUD) >= cloudCost && player.getResources().getCount(Constants.RESOURCE_DATA) >= dataCost)
        {
            player.getResources().removeResource(Constants.RESOURCE_CLOUD, cloudCost);
            player.getResources().removeResource(Constants.RESOURCE_DATA, dataCost);
            
            player.getCompanies().remove(mvp);
            Unicorn unicorn = new Unicorn(player, mvp.getPosition());
            player.getCompanies().add(unicorn);
            mvp.getPosition().setCompany(unicorn);
            
            eventLog.add(player.getName() + " upgraded MVP to Unicorn!");
        }
        else
            throw new InsufficientResourcesException("Not enough resources to upgrade to Unicorn");
    }
    
    public void buildPartnership(Player player, Edge edge, Map<String, Integer> resources) throws InvalidPlacementException, InsufficientResourcesException
    {
        if (edge.hasPartnership())
            throw new InvalidPlacementException("Edge already has a partnership");
        
        if (!player.getResources().hasEnough(resources))
            throw new InsufficientResourcesException("Not enough resources");
        
        player.getResources().deductResources(resources);
        Partnership partnership = new Partnership(player, edge);
        player.getPartnerships().add(partnership);
        edge.setPartnership(partnership);
        
        eventLog.add(player.getName() + " built partnership");
    }
    
    public void endTurn()
    {
        Player currentPlayer = getCurrentPlayer();
        int score = currentPlayer.calculateScore();
        
        if (score >= Constants.WIN_SCORE)
        {
            gameFinished = true;
            winner = currentPlayer;
            eventLog.add(currentPlayer.getName() + " wins with " + score + " points!");
            return;
        }
        
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        market.newTurn();
        eventLog.add("Turn ended. Next player: " + getCurrentPlayer().getName());
    }
    
    
    public Player getCurrentPlayer()  { return players.get(currentPlayerIndex); }
    public Player getWinner()         { return winner; }
    public boolean isGameFinished()   { return gameFinished; }
    public List<String> getEventLog() { return eventLog; }
    public Market getMarket()         { return market; }
    public Sector[][] getMap()        { return map; }
    public Node[][] getNodes()        { return nodes; }
    
    public void assignRole(Player player, String role)
    {
        if (role.equals("Trader"))
            player.setRole("Trader");
        else if (role.equals("CTO"))
            player.setRole("CTO");
        else if (role.equals("VC-Funded"))
        {
            player.setRole("VC-Funded");
            player.getResources().addResource(Constants.RESOURCE_CAPITAL, Constants.VC_EXTRA_CAPITAL);
        }
    }
    
    private boolean placeMVP(Player player, Node node, boolean isFree)
    {
        if (isFree)
        {
            MVP mvp = new MVP(player, node);
            player.getCompanies().add(mvp);
            node.setCompany(mvp);
            return true;
        }
        
        return false;
    }
    
    private boolean placePartnership(Player player, Edge edge, boolean isFree)
    {
        if (isFree && !edge.hasPartnership())
        {
            Partnership partnership = new Partnership(player, edge);
            player.getPartnerships().add(partnership);
            edge.setPartnership(partnership);
            return true;
        }
        
        return false;
    }
}