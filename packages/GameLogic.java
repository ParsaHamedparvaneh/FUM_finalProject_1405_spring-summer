package packages;

import java.io.*;
import java.util.*;

public class GameLogic implements Serializable {
    private static final long serialVersionUID = 2L;
    private List<Player> players;
    private Map<Integer,Player> playerMap;
    private Sector[][] map;
    private Node[][] nodes;
    private Edge[][] horizontalEdges;
    private Edge[][] verticalEdges;
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
        for (int i=0; i<playerNames.size(); i++) {
            Player p = new Player(i+1, playerNames.get(i));
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
        Random rand = new Random();
        for (int i=0; i<Constants.MAP_HEIGHT; i++) {
            for (int j=0; j<Constants.MAP_WIDTH; j++) {
                boolean reg = rand.nextDouble() < 0.1;
                if (reg) map[i][j] = new Sector(i*Constants.SECTOR_SIZE, j*Constants.SECTOR_SIZE, true);
                else map[i][j] = new Sector(i*Constants.SECTOR_SIZE, j*Constants.SECTOR_SIZE);
            }
        }
    }
    
    private void initializeNodesAndEdges() {
        nodes = new Node[Constants.MAP_HEIGHT+1][Constants.MAP_WIDTH+1];
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++)
                nodes[i][j] = new Node(i*Constants.SECTOR_SIZE, j*Constants.SECTOR_SIZE);
        
        for (int i=0; i<Constants.MAP_HEIGHT; i++) {
            for (int j=0; j<Constants.MAP_WIDTH; j++) {
                Sector s = map[i][j];
                s.getNodes()[0] = nodes[i][j];
                s.getNodes()[1] = nodes[i][j+1];
                s.getNodes()[2] = nodes[i+1][j];
                s.getNodes()[3] = nodes[i+1][j+1];
                nodes[i][j].addAdjacentSector(s);
                nodes[i][j+1].addAdjacentSector(s);
                nodes[i+1][j].addAdjacentSector(s);
                nodes[i+1][j+1].addAdjacentSector(s);
            }
        }
        // horizontal edges
        horizontalEdges = new Edge[Constants.MAP_HEIGHT+1][Constants.MAP_WIDTH];
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<Constants.MAP_WIDTH; j++)
                horizontalEdges[i][j] = new Edge(nodes[i][j], nodes[i][j+1]);
        // vertical edges
        verticalEdges = new Edge[Constants.MAP_HEIGHT][Constants.MAP_WIDTH+1];
        for (int i=0; i<Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++)
                verticalEdges[i][j] = new Edge(nodes[i][j], nodes[i+1][j]);
    }
    
    public void startGame() {
        eventLog.add("Game started with " + players.size() + " players.");
        performInitialPlacement();
    }
    
    private void performInitialPlacement() {
        // Round 1: each player places MVP + Partnership (free)
        for (Player p : players) {
            Node node = findAvailableNode();
            if (node != null) {
                MVP mvp = new MVP(p, node);
                p.getCompanies().add(mvp);
                node.setCompany(mvp);
                eventLog.add(p.getName() + " placed free MVP at ("+node.getPosition().x/Constants.SECTOR_SIZE+","+node.getPosition().y/Constants.SECTOR_SIZE+")");
                // place Partnership on an adjacent edge
                Edge edge = findAdjacentFreeEdge(node);
                if (edge != null) {
                    Partnership part = new Partnership(p, edge);
                    p.getPartnerships().add(part);
                    edge.setPartnership(part);
                    eventLog.add(p.getName() + " placed free Partnership.");
                }
            }
        }
        // Round 2: reverse order, again MVP + Partnership
        for (int i=players.size()-1; i>=0; i--) {
            Player p = players.get(i);
            Node node = findAvailableNode();
            if (node != null) {
                MVP mvp = new MVP(p, node);
                p.getCompanies().add(mvp);
                node.setCompany(mvp);
                eventLog.add(p.getName() + " placed second free MVP at ("+node.getPosition().x/Constants.SECTOR_SIZE+","+node.getPosition().y/Constants.SECTOR_SIZE+")");
                Edge edge = findAdjacentFreeEdge(node);
                if (edge != null) {
                    Partnership part = new Partnership(p, edge);
                    p.getPartnerships().add(part);
                    edge.setPartnership(part);
                    eventLog.add(p.getName() + " placed second free Partnership.");
                }
                // Give initial resources from this MVP's adjacent sectors
                for (Sector s : node.getAdjacentSectors()) {
                    String res = s.getResourceType();
                    if (res != null) {
                        p.getResources().addResource(res, 1);
                        eventLog.add(p.getName() + " gained 1 " + res + " from initial placement.");
                    }
                }
            }
        }
        eventLog.add("Initial placement completed.");
    }
    
    private Node findAvailableNode() {
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++)
                if (!nodes[i][j].hasCompany() && isValidPlacement(nodes[i][j]))
                    return nodes[i][j];
        return null;
    }
    
    public boolean isValidPlacement(Node node) {
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++) {
                Node other = nodes[i][j];
                if (other.hasCompany() && other != node) {
                    int dx = Math.abs(node.getPosition().x - other.getPosition().x);
                    int dy = Math.abs(node.getPosition().y - other.getPosition().y);
                    if (dx <= Constants.SECTOR_SIZE*2 && dy <= Constants.SECTOR_SIZE*2)
                        return false;
                }
            }
        return true;
    }
    
    private Edge findAdjacentFreeEdge(Node node) {
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<Constants.MAP_WIDTH; j++)
                if (horizontalEdges[i][j] != null && !horizontalEdges[i][j].hasPartnership() &&
                    (horizontalEdges[i][j].getNode1().equals(node) || horizontalEdges[i][j].getNode2().equals(node)))
                    return horizontalEdges[i][j];
        for (int i=0; i<Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++)
                if (verticalEdges[i][j] != null && !verticalEdges[i][j].hasPartnership() &&
                    (verticalEdges[i][j].getNode1().equals(node) || verticalEdges[i][j].getNode2().equals(node)))
                    return verticalEdges[i][j];
        return null;
    }
    
    public void buildMVP(Player player, Node node) throws InvalidPlacementException, InsufficientResourcesException {
        if (!isValidPlacement(node)) throw new InvalidPlacementException("Too close to another company.");
        if (node.hasCompany()) throw new InvalidPlacementException("Node already occupied.");
        Map<String,Integer> cost = new HashMap<>();
        cost.put(Constants.RESOURCE_TALENT, Constants.MVP_COST_TALENT);
        cost.put(Constants.RESOURCE_CAPITAL, Constants.MVP_COST_CAPITAL);
        cost.put(Constants.RESOURCE_CLOUD, Constants.MVP_COST_CLOUD);
        cost.put(Constants.RESOURCE_DATA, Constants.MVP_COST_DATA);
        if (!player.getResources().hasEnough(cost)) throw new InsufficientResourcesException("Not enough resources.");
        player.getResources().deductResources(cost);
        MVP mvp = new MVP(player, node);
        player.getCompanies().add(mvp);
        node.setCompany(mvp);
        eventLog.add(player.getName() + " built MVP at ("+node.getPosition().x/Constants.SECTOR_SIZE+","+node.getPosition().y/Constants.SECTOR_SIZE+")");
    }
    
    public void upgradeToUnicorn(Player player, MVP mvp) throws InsufficientResourcesException {
        int cloudCost = Constants.UNICORN_UPGRADE_CLOUD;
        int dataCost = Constants.UNICORN_UPGRADE_DATA;
        if (player.getRole() != null && player.getRole().equals("CTO")) cloudCost -= Constants.CLOUD_DISCOUNT;
        if (player.getResources().getCount(Constants.RESOURCE_CLOUD) < cloudCost ||
            player.getResources().getCount(Constants.RESOURCE_DATA) < dataCost)
            throw new InsufficientResourcesException("Need " + cloudCost + " Cloud and " + dataCost + " Data.");
        player.getResources().removeResource(Constants.RESOURCE_CLOUD, cloudCost);
        player.getResources().removeResource(Constants.RESOURCE_DATA, dataCost);
        player.getCompanies().remove(mvp);
        Unicorn uni = new Unicorn(player, mvp.getPosition());
        player.getCompanies().add(uni);
        mvp.getPosition().setCompany(uni);
        eventLog.add(player.getName() + " upgraded MVP to UNICORN!");
    }
    
    public void buildPartnership(Player player, Edge edge) throws InvalidPlacementException, InsufficientResourcesException {
        if (edge.hasPartnership()) throw new InvalidPlacementException("Edge already has a partnership.");
        // check connectivity: edge must touch one of player's companies
        boolean connected = false;
        Node n1 = edge.getNode1(), n2 = edge.getNode2();
        if (n1.hasCompany() && n1.getCompany().getOwner() == player) connected = true;
        if (n2.hasCompany() && n2.getCompany().getOwner() == player) connected = true;
        if (!connected) throw new InvalidPlacementException("Must be connected to your company.");
        Map<String,Integer> cost = new HashMap<>();
        cost.put(Constants.RESOURCE_PATENT, Constants.PARTNERSHIP_COST_PATENT);
        cost.put(Constants.RESOURCE_CAPITAL, Constants.PARTNERSHIP_COST_CAPITAL);
        if (!player.getResources().hasEnough(cost)) throw new InsufficientResourcesException("Not enough resources.");
        player.getResources().deductResources(cost);
        Partnership part = new Partnership(player, edge);
        player.getPartnerships().add(part);
        edge.setPartnership(part);
        eventLog.add(player.getName() + " built a Partnership.");
    }
    
    public List<Edge> getValidPartnershipEdges(Player player) {
        List<Edge> edges = new ArrayList<>();
        for (int i=0; i<=Constants.MAP_HEIGHT; i++)
            for (int j=0; j<Constants.MAP_WIDTH; j++)
                if (!horizontalEdges[i][j].hasPartnership()) edges.add(horizontalEdges[i][j]);
        for (int i=0; i<Constants.MAP_HEIGHT; i++)
            for (int j=0; j<=Constants.MAP_WIDTH; j++)
                if (!verticalEdges[i][j].hasPartnership()) edges.add(verticalEdges[i][j]);
        // filter only those connected to player
        edges.removeIf(e -> {
            Node n1=e.getNode1(), n2=e.getNode2();
            return !( (n1.hasCompany() && n1.getCompany().getOwner()==player) ||
                      (n2.hasCompany() && n2.getCompany().getOwner()==player) );
        });
        return edges;
    }
    
    public void rollDice() {
        Random r = new Random();
        int d1 = r.nextInt(6)+1, d2 = r.nextInt(6)+1;
        currentDiceSum = d1+d2;
        eventLog.add("🎲 " + getCurrentPlayer().getName() + " rolled " + d1 + "+" + d2 + " = " + currentDiceSum);
        if (currentDiceSum == 7) handleRegulatoryCrisis();
        else produceResources();
    }
    
    private void produceResources() {
        boolean any = false;
        for (int i=0; i<Constants.MAP_HEIGHT; i++) {
            for (int j=0; j<Constants.MAP_WIDTH; j++) {
                Sector s = map[i][j];
                if (s.canProduce() && s.getDiceNumber() == currentDiceSum) {
                    String res = s.getResourceType();
                    if (res != null) {
                        Set<Player> producers = new HashSet<>();
                        for (Node n : s.getNodes()) if (n!=null && n.hasCompany()) producers.add(n.getCompany().getOwner());
                        for (Player p : producers) {
                            int amount = 1;
                            for (Company c : p.getCompanies()) {
                                if (c instanceof Unicorn && c.getPosition().equals(s.getNodes()[0])) { amount = 2; break; }
                            }
                            p.getResources().addResource(res, amount);
                            eventLog.add(p.getName() + " gained " + amount + " " + res);
                            any = true;
                        }
                    }
                }
            }
        }
        if (!any) eventLog.add("No production this turn.");
    }
    
    private void handleRegulatoryCrisis() {
        eventLog.add("⚠️ Regulatory Crisis! (sum=7)");
        for (Player p : players) {
            int before = p.getResources().getTotalCards();
            p.payTax();
            int after = p.getResources().getTotalCards();
            if (before > after) eventLog.add(p.getName() + " paid tax: " + before + " → " + after);
        }
        if (auditorPosition != null) map[auditorPosition.x][auditorPosition.y].setAuditor(false);
        for (int i=0; i<Constants.MAP_HEIGHT; i++)
            for (int j=0; j<Constants.MAP_WIDTH; j++) {
                boolean hasCompany = false;
                for (Node n : map[i][j].getNodes()) if (n!=null && n.hasCompany()) { hasCompany = true; break; }
                if (hasCompany) {
                    auditorPosition = new Point(i,j);
                    map[i][j].setAuditor(true);
                    eventLog.add("Auditor placed at ("+i+","+j+")");
                    return;
                }
            }
        auditorPosition = new Point(0,0);
        map[0][0].setAuditor(true);
    }
    
    public void endTurn() {
        Player current = getCurrentPlayer();
        int score = current.calculateScore();
        // bonus for longest partnership chain (simplified: any chain length >=3 gives 2 points)
        int longest = 0;
        for (Player p : players) {
            int chain = getLongestPartnershipChain(p);
            if (chain > longest) longest = chain;
        }
        if (longest >= 3) {
            // find player(s) with that length (first one keeps it – simplified)
            for (Player p : players) {
                if (getLongestPartnershipChain(p) == longest) {
                    eventLog.add(p.getName() + " gets +2 points for longest Partnership chain!");
                    // we add directly to score? Score is dynamic, but we need to add bonus.
                    // For simplicity, we'll add a temporary bonus – but real implementation should store.
                    // I'll add a separate bonus method.
                    addLongestChainBonus(p, 2);
                    break;
                }
            }
        }
        eventLog.add(current.getName() + " ends turn with " + current.calculateScore() + " points.");
        if (current.calculateScore() >= Constants.WIN_SCORE) {
            gameFinished = true;
            winner = current;
            eventLog.add(current.getName() + " WINS!");
            return;
        }
        currentPlayerIndex = (currentPlayerIndex+1) % players.size();
        currentTurn++;
        market.newTurn();
        eventLog.add("Next player: " + getCurrentPlayer().getName());
    }
    
    private int getLongestPartnershipChain(Player player) {
        Map<Partnership, List<Partnership>> adj = new HashMap<>();
        List<Partnership> all = new ArrayList<>(player.getPartnerships());
        for (Partnership p1 : all) {
            adj.put(p1, new ArrayList<>());
            for (Partnership p2 : all) {
                if (p1 == p2) continue;
                if (p1.getEdge().getNode1().equals(p2.getEdge().getNode1()) ||
                    p1.getEdge().getNode1().equals(p2.getEdge().getNode2()) ||
                    p1.getEdge().getNode2().equals(p2.getEdge().getNode1()) ||
                    p1.getEdge().getNode2().equals(p2.getEdge().getNode2())) {
                    adj.get(p1).add(p2);
                }
            }
        }
        Set<Partnership> visited = new HashSet<>();
        int max = 0;
        for (Partnership p : all) {
            if (!visited.contains(p)) {
                int len = bfsLength(p, adj, visited);
                max = Math.max(max, len);
            }
        }
        return max;
    }
    
    private int bfsLength(Partnership start, Map<Partnership,List<Partnership>> adj, Set<Partnership> visited) {
        Queue<Partnership> q = new LinkedList<>();
        Map<Partnership,Integer> dist = new HashMap<>();
        q.add(start);
        dist.put(start,1);
        int max=1;
        while(!q.isEmpty()) {
            Partnership cur = q.poll();
            visited.add(cur);
            int d = dist.get(cur);
            max = Math.max(max, d);
            for (Partnership nb : adj.getOrDefault(cur, new ArrayList<>())) {
                if (!dist.containsKey(nb)) {
                    dist.put(nb, d+1);
                    q.add(nb);
                }
            }
        }
        return max;
    }
    
    private void addLongestChainBonus(Player player, int bonus) {
        // We'll store a temporary bonus in a map or add directly to score via a separate field.
        // For simplicity, I'll add a "bonusScore" field to Player. But to avoid modifying Player class now,
        // we'll just give resources as compensation? No, must be points. So let's add a field to Player.
        // Since Player already has calculateScore, we can add a transient bonusPoints.
        player.addBonusPoints(bonus);
    }
    
    public void assignRole(Player player, String roleName) {
        player.setRole(roleName);
        if (roleName.equals("VC-Funded")) {
            player.getResources().addResource(Constants.RESOURCE_CAPITAL, Constants.VC_EXTRA_CAPITAL);
            eventLog.add(player.getName() + " took VC-Funded role (+2 Capital)");
        } else if (roleName.equals("Trader")) {
            eventLog.add(player.getName() + " took Trader role (better market rates)");
        } else if (roleName.equals("CTO")) {
            eventLog.add(player.getName() + " took CTO role (cheaper Unicorn)");
        }
    }
    
    // Getters, setters, save/load etc.
    public List<Player> getPlayers() { return players; }
    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public Sector[][] getMap() { return map; }
    public Node[][] getNodes() { return nodes; }
    public Market getMarket() { return market; }
    public Player getWinner() { return winner; }
    public boolean isGameFinished() { return gameFinished; }
    public List<String> getEventLog() { return eventLog; }
    public Point getAuditorPosition() { return auditorPosition; }
    public void setAuditorPosition(Point p) { auditorPosition = p; }
    public void setCurrentPlayerIndex(int idx) { currentPlayerIndex = idx; }
    public void setCurrentTurn(int turn) { currentTurn = turn; }
    public void setEventLog(List<String> log) { eventLog = log; }
    public Player getPlayerById(int id) { return playerMap.get(id); }
    
    public void saveGame(String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
        }
    }
    public static GameLogic loadGame(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (GameLogic) ois.readObject();
        }
    }
}