package packages;

import java.io.*;
import java.util.*;

public class GameLogic implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Player> players;
    private Map<Integer, Player> playerMap;
    private Sector[][] map;
    private Node[][] nodes;
    private transient Edge[][] edges; // transient because edges can be reconstructed
    private Market market;
    private int currentPlayerIndex;
    private int currentTurn;
    private int currentDiceSum;
    private Point auditorPosition;
    private boolean gameFinished;
    private Player winner;
    private List<String> eventLog;
    
    public GameLogic(List<String> playerNames) {
        players = new ArrayList<>();
        playerMap = new HashMap<>();
        for (int i = 0; i < playerNames.size(); i++) {
            Player p = new Player(i + 1, playerNames.get(i));
            players.add(p);
            playerMap.put(p.getId(), p);
        }
        
        initializeMap();
        initializeNodesAndEdges();
        market = new Market();
        currentPlayerIndex = 0;
        currentTurn = 0;
        gameFinished = false;
        eventLog = new ArrayList<>();
        auditorPosition = null;
    }
    
    private void initializeMap() {
        map = new Sector[Constants.MAP_HEIGHT][Constants.MAP_WIDTH];
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                // Randomly decide if this is a regulatory zone (10% chance)
                boolean isRegulatory = Math.random() < 0.1;
                if (isRegulatory) {
                    map[i][j] = new Sector(i * Constants.SECTOR_SIZE, j * Constants.SECTOR_SIZE, true);
                } else {
                    map[i][j] = new Sector(i * Constants.SECTOR_SIZE, j * Constants.SECTOR_SIZE);
                }
            }
        }
    }
    
    private void initializeNodesAndEdges() {
        // Create 6x6 nodes (for 5x5 sectors)
        nodes = new Node[Constants.MAP_HEIGHT + 1][Constants.MAP_WIDTH + 1];
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                nodes[i][j] = new Node(i * Constants.SECTOR_SIZE, j * Constants.SECTOR_SIZE);
            }
        }
        
        // Connect nodes to adjacent sectors
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                Sector sector = map[i][j];
                sector.getNodes()[0] = nodes[i][j];       // top-left
                sector.getNodes()[1] = nodes[i][j+1];     // top-right
                sector.getNodes()[2] = nodes[i+1][j];     // bottom-left
                sector.getNodes()[3] = nodes[i+1][j+1];   // bottom-right
                
                // Add sector to nodes' adjacent sectors
                nodes[i][j].addAdjacentSector(sector);
                nodes[i][j+1].addAdjacentSector(sector);
                nodes[i+1][j].addAdjacentSector(sector);
                nodes[i+1][j+1].addAdjacentSector(sector);
            }
        }
        
        // Create edges (horizontal)
        edges = new Edge[Constants.MAP_HEIGHT + 1][Constants.MAP_WIDTH];
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                edges[i][j] = new Edge(nodes[i][j], nodes[i][j+1]);
                // Add adjacent sectors to this edge
                if (i > 0 && i <= Constants.MAP_HEIGHT) {
                    if (j < Constants.MAP_WIDTH) {
                        edges[i][j].addAdjacentSector(map[i-1][j]);
                    }
                }
                if (i < Constants.MAP_HEIGHT) {
                    if (j < Constants.MAP_WIDTH) {
                        edges[i][j].addAdjacentSector(map[i][j]);
                    }
                }
            }
        }
        
        // Create vertical edges
        Edge[][] verticalEdges = new Edge[Constants.MAP_HEIGHT][Constants.MAP_WIDTH + 1];
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                verticalEdges[i][j] = new Edge(nodes[i][j], nodes[i+1][j]);
                if (i < Constants.MAP_HEIGHT) {
                    if (j > 0 && j <= Constants.MAP_WIDTH) {
                        verticalEdges[i][j].addAdjacentSector(map[i][j-1]);
                    }
                    if (j < Constants.MAP_WIDTH) {
                        verticalEdges[i][j].addAdjacentSector(map[i][j]);
                    }
                }
            }
        }
        
        // Combine edges into one array for easier access (optional)
        // For simplicity, we'll just use horizontal edges in this implementation
    }
    
    public void startGame() {
        eventLog.add("🎮 Game started with " + players.size() + " players!");
        performInitialPlacement();
    }
    
    private void performInitialPlacement() {
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        eventLog.add("📍 INITIAL PLACEMENT PHASE");
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Round 1: Each player places MVP and Partnership in order
        for (Player player : players) {
            Node availableNode = findAvailableNodeForPlacement();
            if (availableNode != null) {
                placeMVP(player, availableNode, true);
                Edge adjacentEdge = findAdjacentFreeEdge(availableNode);
                if (adjacentEdge != null) {
                    placePartnership(player, adjacentEdge, true);
                }
            }
        }
        
        // Round 2: Reverse order
        eventLog.add("Round 2 (Reverse Order):");
        for (int i = players.size() - 1; i >= 0; i--) {
            Player player = players.get(i);
            Node availableNode = findAvailableNodeForPlacement();
            if (availableNode != null) {
                placeMVP(player, availableNode, true);
                Edge adjacentEdge = findAdjacentFreeEdge(availableNode);
                if (adjacentEdge != null) {
                    placePartnership(player, adjacentEdge, true);
                }
            }
        }
        
        // Give initial resources from second MVP
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        eventLog.add("📦 Granting initial resources...");
        for (Player player : players) {
            // Find the player's second MVP (most recently placed)
            List<Company> companies = player.getCompanies();
            if (companies.size() >= 2) {
                Company secondMVP = companies.get(companies.size() - 1);
                Node pos = secondMVP.getPosition();
                for (Sector sector : pos.getAdjacentSectors()) {
                    String resource = sector.getResourceType();
                    if (resource != null) {
                        player.getResources().addResource(resource, 1);
                        eventLog.add("  " + player.getName() + " gained 1 " + resource + " from initial placement");
                    }
                }
            }
        }
        
        eventLog.add("✅ Initial placement completed!");
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private Node findAvailableNodeForPlacement() {
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                Node node = nodes[i][j];
                if (!node.hasCompany() && isValidPlacement(node)) {
                    return node;
                }
            }
        }
        return null;
    }
    
    public boolean isValidPlacement(Node node) {
        // Check distance rule: at least 2 edges away from other companies
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j <= Constants.MAP_WIDTH; j++) {
                Node other = nodes[i][j];
                if (other.hasCompany() && other != node) {
                    int dx = Math.abs(node.getPosition().x - other.getPosition().x);
                    int dy = Math.abs(node.getPosition().y - other.getPosition().y);
                    // Need at least 2 edges distance (200 pixels since each sector is 100)
                    if (dx <= Constants.SECTOR_SIZE * 2 && dy <= Constants.SECTOR_SIZE * 2) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private Edge findAdjacentFreeEdge(Node node) {
        // Search for an edge connected to this node
        for (int i = 0; i <= Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                Edge edge = edges[i][j];
                if (edge != null && !edge.hasPartnership() && 
                    (edge.getNode1().equals(node) || edge.getNode2().equals(node))) {
                    return edge;
                }
            }
        }
        return null;
    }
    
    private void placeMVP(Player player, Node node, boolean isFree) {
        MVP mvp = new MVP(player, node);
        player.getCompanies().add(mvp);
        node.setCompany(mvp);
        if (isFree) {
            eventLog.add("  " + player.getName() + " placed free MVP at (" + 
                        node.getPosition().x/Constants.SECTOR_SIZE + "," + 
                        node.getPosition().y/Constants.SECTOR_SIZE + ")");
        }
    }
    
    private void placePartnership(Player player, Edge edge, boolean isFree) {
        Partnership partnership = new Partnership(player, edge);
        player.getPartnerships().add(partnership);
        edge.setPartnership(partnership);
        if (isFree) {
            eventLog.add("  " + player.getName() + " placed free Partnership");
        }
    }
    
    public void rollDice() {
        Random rand = new Random();
        int die1 = rand.nextInt(6) + 1;
        int die2 = rand.nextInt(6) + 1;
        currentDiceSum = die1 + die2;
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        eventLog.add("🎲 " + getCurrentPlayer().getName() + " rolled " + die1 + " + " + die2 + " = " + currentDiceSum);
        
        if (currentDiceSum == 7) {
            handleRegulatoryCrisis();
        } else {
            produceResources();
        }
        eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
    
    private void produceResources() {
        boolean produced = false;
        for (int i = 0; i < Constants.MAP_HEIGHT; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH; j++) {
                Sector sector = map[i][j];
                if (sector.canProduce() && sector.getDiceNumber() == currentDiceSum) {
                    String resource = sector.getResourceType();
                    if (resource != null) {
                        // Find players with companies on adjacent nodes
                        Set<Player> producers = new HashSet<>();
                        Node[] sectorNodes = sector.getNodes();
                        for (Node node : sectorNodes) {
                            if (node != null && node.hasCompany()) {
                                producers.add(node.getCompany().getOwner());
                            }
                        }
                        
                        for (Player player : producers) {
                            int amount = 1;
                            // Check if Unicorn (produces 2)
                            for (Company company : player.getCompanies()) {
                                if (company instanceof Unicorn) {
                                    Node[] companyNodes = sector.getNodes();
                                    for (Node n : companyNodes) {
                                        if (n != null && company.getPosition().equals(n)) {
                                            amount = 2;
                                            break;
                                        }
                                    }
                                }
                            }
                            player.getResources().addResource(resource, amount);
                            eventLog.add("  📦 " + player.getName() + " gained " + amount + " " + resource + " from sector (" + i + "," + j + ")");
                            produced = true;
                        }
                    }
                }
            }
        }
        if (!produced) {
            eventLog.add("  No sectors produced resources this turn.");
        }
    }
    
    private void handleRegulatoryCrisis() {
        eventLog.add("⚠️ REGULATORY CRISIS! (Sum = 7) ⚠️");
        
        // Apply tax to all players
        eventLog.add("💰 TAX COLLECTION:");
        for (Player player : players) {
            int beforeTax = player.getResources().getTotalCards();
            player.payTax();
            int afterTax = player.getResources().getTotalCards();
            if (beforeTax > player.getMaxCardsBeforeTax()) {
                eventLog.add("  " + player.getName() + " had " + beforeTax + " cards, paid tax → " + afterTax + " cards");
            } else {
                eventLog.add("  " + player.getName() + " had " + beforeTax + " cards, below threshold, no tax");
            }
        }
        
        // Move auditor
        if (auditorPosition != null) {
            Sector oldSector = map[auditorPosition.x][auditorPosition.y];
            if (oldSector != null) {
                oldSector.setAuditor(false);
            }
        }
        
        // Find a valid sector with at least one company
        boolean auditorPlaced = false;
        for (int i = 0; i < Constants.MAP_HEIGHT && !auditorPlaced; i++) {
            for (int j = 0; j < Constants.MAP_WIDTH && !auditorPlaced; j++) {
                Sector sector = map[i][j];
                for (Node node : sector.getNodes()) {
                    if (node != null && node.hasCompany()) {
                        auditorPosition = new Point(i, j);
                        sector.setAuditor(true);
                        eventLog.add("🔍 Auditor placed at sector (" + i + "," + j + ") - This sector will not produce resources!");
                        auditorPlaced = true;
                        break;
                    }
                }
            }
        }
        
        if (!auditorPlaced) {
            // Place on any sector if none have companies
            for (int i = 0; i < Constants.MAP_HEIGHT && !auditorPlaced; i++) {
                for (int j = 0; j < Constants.MAP_WIDTH && !auditorPlaced; j++) {
                    auditorPosition = new Point(i, j);
                    map[i][j].setAuditor(true);
                    eventLog.add("🔍 Auditor placed at sector (" + i + "," + j + ")");
                    auditorPlaced = true;
                }
            }
        }
    }
    
    public void buildMVP(Player player, Node position, Map<String, Integer> resources) 
            throws InvalidPlacementException, InsufficientResourcesException {
        
        if (!isValidPlacement(position)) {
            throw new InvalidPlacementException("Invalid placement location: Must be at least 2 edges away from other companies");
        }
        
        if (position.hasCompany()) {
            throw new InvalidPlacementException("This node already has a company");
        }
        
        if (!player.getResources().hasEnough(resources)) {
            throw new InsufficientResourcesException("Not enough resources to build MVP");
        }
        
        player.getResources().deductResources(resources);
        MVP mvp = new MVP(player, position);
        player.getCompanies().add(mvp);
        position.setCompany(mvp);
        
        eventLog.add("🏗️ " + player.getName() + " built MVP at (" + 
                    position.getPosition().x/Constants.SECTOR_SIZE + "," + 
                    position.getPosition().y/Constants.SECTOR_SIZE + ")");
    }
    
    public void upgradeToUnicorn(Player player, MVP mvp) 
            throws InsufficientResourcesException {
        
        int cloudCost = Constants.UNICORN_UPGRADE_CLOUD;
        int dataCost = Constants.UNICORN_UPGRADE_DATA;
        
        // Check for CTO discount
        if (player.getRole() != null && player.getRole().equals("CTO")) {
            cloudCost -= Constants.CLOUD_DISCOUNT;
            eventLog.add("  CTO discount applied! Cloud cost reduced to " + cloudCost);
        }
        
        if (player.getResources().getCount(Constants.RESOURCE_CLOUD) >= cloudCost &&
            player.getResources().getCount(Constants.RESOURCE_DATA) >= dataCost) {
            
            player.getResources().removeResource(Constants.RESOURCE_CLOUD, cloudCost);
            player.getResources().removeResource(Constants.RESOURCE_DATA, dataCost);
            
            // Replace MVP with Unicorn
            player.getCompanies().remove(mvp);
            Unicorn unicorn = new Unicorn(player, mvp.getPosition());
            player.getCompanies().add(unicorn);
            mvp.getPosition().setCompany(unicorn);
            
            eventLog.add("🦄 " + player.getName() + " upgraded MVP to UNICORN! (+2 points)");
        } else {
            throw new InsufficientResourcesException("Not enough resources to upgrade to Unicorn. Need " + 
                cloudCost + " Cloud and " + dataCost + " Data");
        }
    }
    
    public void buildPartnership(Player player, Edge edge, Map<String, Integer> resources)
            throws InvalidPlacementException, InsufficientResourcesException {
        
        if (edge.hasPartnership()) {
            throw new InvalidPlacementException("Edge already has a partnership");
        }
        
        // Check if connected to player's existing structures
        boolean connected = false;
        if (edge.getNode1().hasCompany() && edge.getNode1().getCompany().getOwner() == player) {
            connected = true;
        }
        if (edge.getNode2().hasCompany() && edge.getNode2().getCompany().getOwner() == player) {
            connected = true;
        }
        
        if (!connected) {
            throw new InvalidPlacementException("Partnership must be connected to one of your existing companies");
        }
        
        if (!player.getResources().hasEnough(resources)) {
            throw new InsufficientResourcesException("Not enough resources to build Partnership");
        }
        
        player.getResources().deductResources(resources);
        Partnership partnership = new Partnership(player, edge);
        player.getPartnerships().add(partnership);
        edge.setPartnership(partnership);
        
        eventLog.add("🤝 " + player.getName() + " built a Partnership!");
    }
    
    public void endTurn() {
        Player currentPlayer = getCurrentPlayer();
        
        // Calculate longest partnership chain for bonus
        int longestChain = calculateLongestPartnershipChain();
        for (Player player : players) {
            int playerChain = player.getLongestPartnershipChain(getPartnershipAdjacency());
            if (playerChain >= 3 && playerChain == longestChain) {
                // In a real implementation, track who had it first
                eventLog.add("🏆 " + player.getName() + " has the longest partnership chain (" + playerChain + ")");
            }
        }
        
        int score = currentPlayer.calculateScore();
        eventLog.add("📊 " + currentPlayer.getName() + " ends turn with " + score + " points");
        
        if (score >= Constants.WIN_SCORE) {
            gameFinished = true;
            winner = currentPlayer;
            eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            eventLog.add("🏆🏆🏆 " + currentPlayer.getName().toUpperCase() + " WINS THE GAME! 🏆🏆🏆");
            eventLog.add("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }
        
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        currentTurn++;
        market.newTurn();
        eventLog.add("➡️ Turn ended. Next player: " + getCurrentPlayer().getName());
        eventLog.add("");
    }
    
    private int calculateLongestPartnershipChain() {
        int maxChain = 0;
        for (Player player : players) {
            int chain = player.getLongestPartnershipChain(getPartnershipAdjacency());
            maxChain = Math.max(maxChain, chain);
        }
        return maxChain;
    }
    
    private Map<Partnership, List<Partnership>> getPartnershipAdjacency() {
        Map<Partnership, List<Partnership>> adjacency = new HashMap<>();
        
        // Collect all partnerships
        List<Partnership> allPartnerships = new ArrayList<>();
        for (Player player : players) {
            allPartnerships.addAll(player.getPartnerships());
        }
        
        // Build adjacency based on shared nodes
        for (Partnership p1 : allPartnerships) {
            adjacency.putIfAbsent(p1, new ArrayList<>());
            for (Partnership p2 : allPartnerships) {
                if (p1 != p2) {
                    if (p1.getEdge().getNode1().equals(p2.getEdge().getNode1()) ||
                        p1.getEdge().getNode1().equals(p2.getEdge().getNode2()) ||
                        p1.getEdge().getNode2().equals(p2.getEdge().getNode1()) ||
                        p1.getEdge().getNode2().equals(p2.getEdge().getNode2())) {
                        adjacency.get(p1).add(p2);
                    }
                }
            }
        }
        
        return adjacency;
    }
    
    public void assignRole(Player player, String roleName) {
        player.setRole(roleName);
        
        // Apply role benefits
        if (roleName.equals("VC-Funded")) {
            player.getResources().addResource(Constants.RESOURCE_CAPITAL, Constants.VC_EXTRA_CAPITAL);
            eventLog.add("💎 " + player.getName() + " takes the VC-Funded role! +" + Constants.VC_EXTRA_CAPITAL + " Capital");
        } else if (roleName.equals("Trader")) {
            eventLog.add("💎 " + player.getName() + " takes the Trader role! Better market rates");
        } else if (roleName.equals("CTO")) {
            eventLog.add("💎 " + player.getName() + " takes the CTO role! Cheaper Unicorn upgrades");
        }
        
        eventLog.add("  (Role penalty: -1 point)");
    }
    
    // Getters
    public List<Player> getPlayers() { return players; }
    public Player getPlayerById(int id) { return playerMap.get(id); }
    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int index) { this.currentPlayerIndex = index; }
    public int getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(int turn) { this.currentTurn = turn; }
    public Point getAuditorPosition() { return auditorPosition; }
    public void setAuditorPosition(Point pos) { this.auditorPosition = pos; }
    public List<String> getEventLog() { return eventLog; }
    public void setEventLog(List<String> log) { this.eventLog = log; }
    public Sector[][] getMap() { return map; }
    public Node[][] getNodes() { return nodes; }
    public Market getMarket() { return market; }
    public Player getWinner() { return winner; }
    public boolean isGameFinished() { return gameFinished; }
    
    public void setGameFinished(boolean finished) { this.gameFinished = finished; }
    public void setWinner(Player winner) { this.winner = winner; }
    
    // Save/Load methods
    public void saveGame(String filepath) throws IOException {
        SaveLoadManager.saveGame(this, filepath);
    }
    
    public static GameLogic loadGame(String filepath) throws IOException, ClassNotFoundException {
        return SaveLoadManager.loadGame(filepath);
    }
}