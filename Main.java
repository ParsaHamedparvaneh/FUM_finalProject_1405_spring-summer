import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.util.*;
import java.io.*;

class GameException extends Exception {
    public GameException(String message) { super(message); }
    public GameException(String message, Throwable cause) { super(message, cause); }
}

class IllegalActionException extends GameException {
    public IllegalActionException(String message) { super(message); }
}


class InvalidPlacementException extends GameException {
    public InvalidPlacementException(String message) { super(message); }
}

class InsufficientResourcesException extends GameException {
    public InsufficientResourcesException(String message) { super(message); }
}

class IllegalTradeException extends GameException {
    public IllegalTradeException(String message) { super(message); }
}


class PersistenceException extends GameException {
    public PersistenceException(String message, Throwable cause) { super(message, cause); }
}

enum Resource {
    CAPITAL("Capital"),
    TALENT("Talent"),
    CLOUD("Cloud"),
    DATA("Data"),
    PATENT("Patent");

    private final String display;
    Resource(String display) { this.display = display; }
    public String display() { return display; }
}

enum SectorType {
    HUB_AI("Hub AI", Resource.TALENT),
    DISTRICT_FINTECH("District Fintech", Resource.CAPITAL),
    CAMPUS_CLOUD("Campus Cloud", Resource.CLOUD),
    QUARTER_IP("Quarter IP", Resource.PATENT),
    VALLEY_DATA("Valley Data", Resource.DATA),
    ZONE_REGULATORY("Zone Regulatory", null);

    private final String display;
    private final Resource produces;

    SectorType(String display, Resource produces) {
        this.display = display;
        this.produces = produces;
    }

    public String display() { return display; }
    public Resource produces() { return produces; }
    public boolean isProductive() { return produces != null; }

    public static SectorType[] productiveTypes()
    {
        return new SectorType[] {
            HUB_AI, DISTRICT_FINTECH, CAMPUS_CLOUD, QUARTER_IP, VALLEY_DATA
        };
    }

}

enum FounderRole {
    NONE("No Role", 0),
    CEO_HACKER("CEO Hacker", GameConstants.ROLE_SCORE_PENALTY),
    TECH_GURU("Tech Guru (CTO)", GameConstants.ROLE_SCORE_PENALTY),
    VC_FUNDED("VC-Funded", GameConstants.ROLE_SCORE_PENALTY);

    private final String display;
    private final int scorePenalty;

    FounderRole(String display, int scorePenalty)
    {
        this.display = display;
        this.scorePenalty = scorePenalty;
    }

    public String display() { return display; }
    public int scorePenalty() { return scorePenalty; }

    public int crisisCardLimit()
    {
        switch (this)
        {
            case TECH_GURU:
            case VC_FUNDED:
                return GameConstants.CRISIS_LIMIT_PRIVILEGED;
            default:
                return GameConstants.CRISIS_LIMIT_DEFAULT;
        }
    }

    public int marketTradeRatio()
    {
        return this == CEO_HACKER
                ? GameConstants.MARKET_RATIO_HACKER
                : GameConstants.MARKET_RATIO_STANDARD;
    }
}


class GameConstants {

    private GameConstants() { }

    // --- Dice ---
    public static final int DICE_MIN_FACE = 1;
    public static final int DICE_MAX_FACE = 6;
    public static final int DICE_COUNT = 2;
    public static final int CRISIS_DICE_SUM = 7;
    public static final int MIN_DICE_SUM = 2;
    public static final int MAX_DICE_SUM = 12;

    // --- Victory ---
    public static final int VICTORY_POINTS = 10;

    // --- Structure point values ---
    public static final int MVP_POINTS = 1;
    public static final int UNICORN_POINTS = 2;

    // --- Crisis ---
    public static final int CRISIS_LIMIT_DEFAULT = 7;
    public static final int CRISIS_LIMIT_PRIVILEGED = 9;
    public static final int CRISIS_DISCARD_DIVISOR = 2;

    // --- Roles ---
    public static final int ROLE_SCORE_PENALTY = -1;
    public static final int TECH_GURU_BONUS_CAPITAL = 2;
    public static final int MARKET_RATIO_STANDARD = 4;
    public static final int MARKET_RATIO_HACKER = 3;
    public static final int HACKER_UNICORN_CLOUD_DISCOUNT = 1;

    // --- Market pricing ---
    public static final int MARKET_BASE_PRICE = 4;
    public static final int MARKET_MAX_PRICE = 6;
    public static final int MARKET_MIN_PRICE = 2;
    public static final int MARKET_PRICE_STEP = 1;
    public static final int MARKET_IDLE_TURNS_FOR_DROP = 3;

    // --- Map dimensions (square grid of sectors) ---
    public static final int MAP_ROWS = 5;
    public static final int MAP_COLS = 5;

    // --- Build costs (immutable) ---
    public static final Map<Resource, Integer> MVP_COST;
    public static final Map<Resource, Integer> UNICORN_COST;
    public static final Map<Resource, Integer> PARTNERSHIP_COST;

    static {
        Map<Resource, Integer> mvp = new EnumMap<>(Resource.class);
        mvp.put(Resource.CAPITAL, 1);
        mvp.put(Resource.TALENT, 1);
        mvp.put(Resource.CLOUD, 1);
        mvp.put(Resource.DATA, 1);
        MVP_COST = Collections.unmodifiableMap(mvp);

        Map<Resource, Integer> unicorn = new EnumMap<>(Resource.class);
        unicorn.put(Resource.DATA, 3);
        unicorn.put(Resource.CLOUD, 2);
        UNICORN_COST = Collections.unmodifiableMap(unicorn);

        Map<Resource, Integer> partnership = new EnumMap<>(Resource.class);
        partnership.put(Resource.CAPITAL, 1);
        partnership.put(Resource.PATENT, 1);
        PARTNERSHIP_COST = Collections.unmodifiableMap(partnership);
    }

    public static final int DEFAULT_CARD_LIMIT = CRISIS_LIMIT_DEFAULT;
    public static final int RAISED_CARD_LIMIT = CRISIS_LIMIT_PRIVILEGED;
    public static final int LONGEST_NETWORK_POINTS = 2;
    public static final int ROLE_PENALTY = ROLE_SCORE_PENALTY;
    public static final int BASE_PRICE = MARKET_BASE_PRICE;
    public static final int MAX_PRICE = MARKET_MAX_PRICE;
    public static final int MIN_PRICE = MARKET_MIN_PRICE;
    public static final int PRICE_DECAY_TURNS = MARKET_IDLE_TURNS_FOR_DROP;
    public static final int MIN_ACTIVATION = MIN_DICE_SUM;
    public static final int MAX_ACTIVATION = MAX_DICE_SUM;
    public static final int CRISIS_ROLL = CRISIS_DICE_SUM;
    public static final int LONGEST_NETWORK_MIN = 3;

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

}

abstract class CompanyStructure implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final int ownerId;

    protected CompanyStructure(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public abstract Map<Resource, Integer> produce(Resource resource);

    public abstract int getVictoryPoints();

    public abstract String typeName();
}

class MVP extends CompanyStructure {
    private static final long serialVersionUID = 1L;
    private static final int PRODUCTION_UNITS = 1;

    public MVP(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        Map<Resource, Integer> out = new EnumMap<>(Resource.class);
        if (resource != null) {
            out.put(resource, PRODUCTION_UNITS);
        }
        return out;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.MVP_POINTS;
    }

    @Override
    public String typeName() {
        return "MVP";
    }
}

class Unicorn extends CompanyStructure {
    private static final long serialVersionUID = 1L;
    private static final int PRODUCTION_UNITS = 2;

    public Unicorn(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        Map<Resource, Integer> out = new EnumMap<>(Resource.class);
        if (resource != null) {
            out.put(resource, PRODUCTION_UNITS);
        }
        return out;
    }

    @Override
    public int getVictoryPoints() {
        return GameConstants.UNICORN_POINTS;
    }

    @Override
    public String typeName() {
        return "Unicorn";
    }
}

class Partnership extends CompanyStructure {
    private static final long serialVersionUID = 1L;

    public Partnership(int ownerId) {
        super(ownerId);
    }

    @Override
    public Map<Resource, Integer> produce(Resource resource) {
        return Collections.emptyMap();
    }

    @Override
    public int getVictoryPoints() {
        return 0;
    }

    @Override
    public String typeName() {
        return "Partnership";
    }
}

class Sector implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int row;
    private final int col;
    private final SectorType type;
    private final int activationNumber;
    private boolean inspected;

    public Sector(int row, int col, SectorType type, int activationNumber) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.activationNumber = activationNumber;
        this.inspected = false;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public SectorType getType() { return type; }
    public int getActivationNumber() { return activationNumber; }

    public boolean isInspected() { return inspected; }
    public void setInspected(boolean inspected) { this.inspected = inspected; }

    public boolean producesOn(int diceSum) {
        return type.isProductive() && !inspected && activationNumber == diceSum;
    }

    @Override
    public String toString() {
        return type.display() + (type.isProductive() ? " [" + activationNumber + "]" : "");
    }
}

class Vertex implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final List<Sector> adjacentSectors;
    private final List<Integer> adjacentVertexIds;
    private CompanyStructure structure;

    public Vertex(int id) {
        this.id = id;
        this.adjacentSectors = new ArrayList<>();
        this.adjacentVertexIds = new ArrayList<>();
        this.structure = null;
    }

    public int getId() { return id; }
    public List<Sector> getAdjacentSectors() { return adjacentSectors; }
    public List<Integer> getAdjacentVertexIds() { return adjacentVertexIds; }

    public CompanyStructure getStructure() { return structure; }
    public void setStructure(CompanyStructure structure) { this.structure = structure; }
    public boolean isOccupied() { return structure != null; }

    public void linkSector(Sector sector) {
        if (sector != null && !adjacentSectors.contains(sector)) {
            adjacentSectors.add(sector);
        }
    }

    public void linkVertex(int otherId) {
        if (otherId != id && !adjacentVertexIds.contains(otherId)) {
            adjacentVertexIds.add(otherId);
        }
    }
}

class Edge implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int vertexA;
    private final int vertexB;
    private Partnership partnership;

    public Edge(int id, int vertexA, int vertexB) {
        this.id = id;
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.partnership = null;
    }

    public int getId() { return id; }
    public int getVertexA() { return vertexA; }
    public int getVertexB() { return vertexB; }

    public Partnership getPartnership() { return partnership; }
    public void setPartnership(Partnership partnership) { this.partnership = partnership; }
    public boolean hasPartnership() { return partnership != null; }


    public boolean connectsBoth(int a, int b)
    {
        return (vertexA == a && vertexB == b) || (vertexA == b && vertexB == a);
    }


    public boolean connects(int vertexId) {
        return vertexA == vertexId || vertexB == vertexId;
    }

    public int other(int vertexId) {
        if (vertexA == vertexId) return vertexB;
        if (vertexB == vertexId) return vertexA;
        return -1;
    }
}

class ResourceBank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Resource, Integer> cards;

    public ResourceBank() {
        cards = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            cards.put(r, 0);
        }
    }

    public int get(Resource resource) {
        return cards.getOrDefault(resource, 0);
    }

    public void add(Resource resource, int amount) {
        cards.put(resource, get(resource) + amount);
    }

    public boolean has(Resource resource, int amount) {
        return get(resource) >= amount;
    }

    public boolean remove(Resource resource, int amount) {
        if (!has(resource, amount)) {
            return false;
        }
        cards.put(resource, get(resource) - amount);
        return true;
    }

    public int total() {
        int sum = 0;
        for (int v : cards.values()) {
            sum += v;
        }
        return sum;
    }

    public Map<Resource, Integer> snapshot() {
        return new EnumMap<>(cards);
    }

    public boolean canAfford(Map<Resource, Integer> cost) {
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            if (get(e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void pay(Map<Resource, Integer> cost) {
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            remove(e.getKey(), e.getValue());
        }
    }
}

class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int pendingTax;
    private final int id;
    private final String name;
    private final boolean ai;
    private final ResourceBank bank;
    private final List<Vertex> ownedVertices;
    private final List<Edge> ownedEdges;
    private FounderRole role;
    private int longestNetworkAchievedAtTurn;
    private boolean holdsLongestNetwork;

    public Player(int id, String name, boolean ai) {
        this.id = id;
        this.pendingTax = 0;
        this.name = name;
        this.ai = ai;
        this.bank = new ResourceBank();
        this.ownedVertices = new ArrayList<>();
        this.ownedEdges = new ArrayList<>();
        this.role = FounderRole.NONE;
        this.longestNetworkAchievedAtTurn = Integer.MAX_VALUE;
        this.holdsLongestNetwork = false;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isAi() { return ai; }
    public ResourceBank getBank() { return bank; }
    public List<Vertex> getOwnedVertices() { return ownedVertices; }
    public List<Edge> getOwnedEdges() { return ownedEdges; }

    public FounderRole getRole() { return role; }
    public void setRole(FounderRole role) { this.role = role; }

    public boolean holdsLongestNetwork() { return holdsLongestNetwork; }
    public void setHoldsLongestNetwork(boolean v) { this.holdsLongestNetwork = v; }

    public int getLongestNetworkAchievedAtTurn() { return longestNetworkAchievedAtTurn; }
    public void setLongestNetworkAchievedAtTurn(int turn) { this.longestNetworkAchievedAtTurn = turn; }

    public int getPendingTax() { return pendingTax; }
    public void markPendingTax(int amount) { this.pendingTax = amount; }
    public void clearPendingTax() { this.pendingTax = 0; }

    public void addResource(Resource r, int amount) { bank.add(r, amount); }
    public boolean removeResource(Resource r, int amount) { return bank.remove(r, amount); }
    public int getResource(Resource r) { return bank.get(r); }
    public int totalCards() { return bank.total(); }

    public int getCardLimit() {
        if (role == FounderRole.TECH_GURU || role == FounderRole.VC_FUNDED) {
            return GameConstants.RAISED_CARD_LIMIT;
        }
        return GameConstants.DEFAULT_CARD_LIMIT;
    }

    public void registerStructureVertex(Vertex v) {
        if (!ownedVertices.contains(v)) {
            ownedVertices.add(v);
        }
    }

    public void registerEdge(Edge e) {
        if (!ownedEdges.contains(e)) {
            ownedEdges.add(e);
        }
    }

    public void grantProduction(Map<Resource, Integer> produced) {
        for (Map.Entry<Resource, Integer> e : produced.entrySet()) {
            bank.add(e.getKey(), e.getValue());
        }
    }

    public int computeScore() {
        int score = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() != null) {
                score += v.getStructure().getVictoryPoints();
            }
        }
        if (holdsLongestNetwork) {
            score += GameConstants.LONGEST_NETWORK_POINTS;
        }
        if (role != FounderRole.NONE) {
            score += GameConstants.ROLE_PENALTY;
        }
        return score;
    }

    public int mvpCount() {
        int c = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() instanceof MVP) c++;
        }
        return c;
    }

    public int unicornCount() {
        int c = 0;
        for (Vertex v : ownedVertices) {
            if (v.getStructure() instanceof Unicorn) c++;
        }
        return c;
    }
}

class Market implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Resource, Integer> prices;
    private final Map<Resource, Integer> turnsSinceLastBuy;

    public Market() {
        prices = new EnumMap<>(Resource.class);
        turnsSinceLastBuy = new EnumMap<>(Resource.class);
        for (Resource r : Resource.values()) {
            prices.put(r, GameConstants.BASE_PRICE);
            turnsSinceLastBuy.put(r, 0);
        }
    }

    public int priceOf(Resource resource) {
        return prices.get(resource);
    }

    public Map<Resource, Integer> snapshot() {
        return new EnumMap<>(prices);
    }

    public void onBuy(Resource resource) {
        int p = Math.min(GameConstants.MAX_PRICE, prices.get(resource) + 1);
        prices.put(resource, p);
        turnsSinceLastBuy.put(resource, 0);
    }

    public void onTurnElapsed() {
        for (Resource r : Resource.values()) {
            int counter = turnsSinceLastBuy.get(r) + 1;
            if (counter >= GameConstants.PRICE_DECAY_TURNS) {
                int p = Math.max(GameConstants.MIN_PRICE, prices.get(r) - 1);
                prices.put(r, p);
                turnsSinceLastBuy.put(r, 0);
            } else {
                turnsSinceLastBuy.put(r, counter);
            }
        }
    }
}

class GameMap implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int rows;
    private final int cols;
    private final Sector[][] sectors;
    private final Map<Integer, Vertex> vertices;
    private final Map<Integer, Edge> edges;
    private final Map<Long, Integer> vertexKeyToId;

    public GameMap(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.sectors = new Sector[rows][cols];
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        this.vertexKeyToId = new HashMap<>();
        generate(new Random(seed));
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Sector[][] getSectors() { return sectors; }
    public Map<Integer, Vertex> getVertices() { return vertices; }
    public Map<Integer, Edge> getEdges() { return edges; }

    public List<Resource> adjacentResources(int vertexId) {
        List<Resource> out = new ArrayList<>();
        for (Sector s : vertices.get(vertexId).getAdjacentSectors()) {
            if (s.getType().isProductive()) out.add(s.getType().produces());
        }
        return out;
    }

    public List<Sector> sectorsWithActivation(int diceSum) {
        List<Sector> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (sectors[r][c].getActivationNumber() == diceSum
                        && sectors[r][c].getType().isProductive()) {
                    out.add(sectors[r][c]);
                }
            }
        }
        return out;
    }

    public List<Vertex> verticesAround(Sector sector) {
        List<Vertex> out = new ArrayList<>();
        for (Vertex v : vertices.values()) {
            if (v.getAdjacentSectors().contains(sector)) out.add(v);
        }
        return out;
    }

    public boolean sectorHasCompany(Sector sector) {
        for (Vertex v : verticesAround(sector)) {
            if (v.isOccupied()) return true;
        }
        return false;
    }

    public boolean anySectorOccupied() {
        for (Vertex v : vertices.values()) {
            if (v.isOccupied()) return true;
        }
        return false;
    }


    private void generate(Random random) {
        List<SectorType> pool = buildTypePool(random);
        List<Integer> numbers = buildNumberPool(random);
        int poolIndex = 0;
        int numberIndex = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                SectorType type = pool.get(poolIndex % pool.size());
                poolIndex++;
                int number;
                if (type.isProductive()) {
                    number = numbers.get(numberIndex % numbers.size());
                    numberIndex++;
                } else {
                    number = 0;
                }
                sectors[r][c] = new Sector(r, c, type, number);
            }
        }
        buildVerticesAndEdges();
    }

    private List<SectorType> buildTypePool(Random random) {
        List<SectorType> pool = new ArrayList<>();
        SectorType[] productive = SectorType.productiveTypes();
        int totalCells = rows * cols;
        int regulatoryCount = Math.max(1, totalCells / 7);
        for (int i = 0; i < regulatoryCount; i++) {
            pool.add(SectorType.ZONE_REGULATORY);
        }
        int remaining = totalCells - regulatoryCount;
        int idx = 0;
        for (int i = 0; i < remaining; i++) {
            pool.add(productive[idx % productive.length]);
            idx++;
        }
        Collections.shuffle(pool, random);
        return pool;
    }

    private List<Integer> buildNumberPool(Random random) {
        List<Integer> numbers = new ArrayList<>();
        int total = rows * cols;
        for (int i = 0; i < total; i++) {
            int n = GameConstants.MIN_ACTIVATION + random.nextInt(GameConstants.MAX_ACTIVATION - GameConstants.MIN_ACTIVATION + 1);
            if (n == GameConstants.CRISIS_ROLL) {
                n = n == GameConstants.MAX_ACTIVATION ? n - 1 : n + 1;
            }
            numbers.add(n);
        }
        Collections.shuffle(numbers, random);
        return numbers;
    }

    private long vertexKey(int r, int c) {
        return ((long) r << 32) | (c & 0xffffffffL);
    }

        private int obtainVertex(int r, int c) {
        long key = vertexKey(r, c);
        Integer existing = vertexKeyToId.get(key);
        if (existing != null) {
            return existing;
        }
        int id = vertices.size();
        Vertex v = new Vertex(id);
        vertices.put(id, v);
        v.linkVertex(obtainVertexPreemptive(r - 1, c));
        v.linkVertex(obtainVertexPreemptive(r + 1, c));
        v.linkVertex(obtainVertexPreemptive(r, c - 1));
        v.linkVertex(obtainVertexPreemptive(r, c + 1));
        vertexKeyToId.put(key, id);
        return id;
    }

    private int obtainVertexPreemptive(int r, int c) {
        if (r < 0 || r > rows || c < 0 || c > cols) return -1;
        long key = vertexKey(r, c);
        Integer id = vertexKeyToId.get(key);
        return (id != null) ? id : -1;
    }

    private void buildVerticesAndEdges() {
        for (int r = 0; r <= rows; r++) {
            for (int c = 0; c <= cols; c++) {
                int vid = obtainVertex(r, c);
                Vertex v = vertices.get(vid);
                if (r > 0 && c > 0) v.linkSector(sectors[r - 1][c - 1]);
                if (r > 0 && c < cols) v.linkSector(sectors[r - 1][c]);
                if (r < rows && c > 0) v.linkSector(sectors[r][c - 1]);
                if (r < rows && c < cols) v.linkSector(sectors[r][c]);
            }
        }
        Set<String> seenEdges = new HashSet<>();
        for (Vertex v : vertices.values()) {
            for (int neighborId : v.getAdjacentVertexIds()) {
                if (neighborId == -1) continue;
                int low = Math.min(v.getId(), neighborId);
                int high = Math.max(v.getId(), neighborId);
                String edgeKey = low + "-" + high;
                if (!seenEdges.contains(edgeKey)) {
                    int eid = edges.size();
                    edges.put(eid, new Edge(eid, low, high));
                    seenEdges.add(edgeKey);
                }
            }
        }
    }

    public List<Edge> getVertexEdges(int vertexId) {
        List<Edge> out = new ArrayList<>();
        for (Edge e : edges.values()) {
            if (e.connects(vertexId)) out.add(e);
        }
        return out;
    }

    public List<Integer> neighborsOf(int vertexId) {
        Vertex v = vertices.get(vertexId);
        List<Integer> out = new ArrayList<>();
        for (int id : v.getAdjacentVertexIds()) {
            if (id != -1) out.add(id);
        }
        return out;
    }

    public int distanceInVertices(int fromVertexId, int toVertexId) {
        if (fromVertexId == toVertexId) return 0;
        Deque<int[]> queue = new ArrayDeque<>();
        Set<Integer> visited = new HashSet<>();
        queue.add(new int[]{fromVertexId, 0});
        visited.add(fromVertexId);
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int next : neighborsOf(cur[0])) {
                if (visited.contains(next)) continue;
                if (next == toVertexId) return cur[1] + 1;
                visited.add(next);
                queue.add(new int[]{next, cur[1] + 1});
            }
        }
        return Integer.MAX_VALUE;
    }

    public Edge edgeBetween(int a, int b) {
        for (Edge e : edges.values()) {
            if (e.connectsBoth(a, b)) return e;
        }
        return null;
    }
}

class GameController implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final GameMap gameMap;
    private final Market market;
    private final List<Player> players;
    private final Random random;

    private int currentPlayerIndex;
    private int turnCount;
    private int lastDiceRoll;
    private Sector inspectorSector;
    private boolean gameOver;
    private Player winner;
    private int longestNetworkHolder;
    private int longestNetworkLength;

    private final List<String> eventLog;

    public GameController(List<String> playerNames, long seed)
    {
        if (playerNames == null || playerNames.size() < GameConstants.MIN_PLAYERS
                || playerNames.size() > GameConstants.MAX_PLAYERS)
        {
            throw new IllegalArgumentException("Player count must be between "
                    + GameConstants.MIN_PLAYERS + " and " + GameConstants.MAX_PLAYERS);
        }
        this.random = new Random(seed);
        this.gameMap = new GameMap(GameConstants.MAP_ROWS, GameConstants.MAP_COLS, seed);
        this.market = new Market();
        this.players = new ArrayList<>();
        for (int i = 0; i < playerNames.size(); i++)
            players.add(new Player(i, playerNames.get(i), false));
        this.currentPlayerIndex = 0;
        this.turnCount = 0;
        this.lastDiceRoll = 0;
        this.inspectorSector = null;
        this.gameOver = false;
        this.winner = null;
        this.longestNetworkHolder = -1;
        this.longestNetworkLength = 0;
        this.eventLog = new ArrayList<>();
    }

    private void validateVertexForBuild(Player player, int vertexId, boolean setup) throws GameException {
        Vertex v = gameMap.getVertices().get(vertexId);
        if (v == null) throw new InvalidPlacementException("Unknown vertex");
        if (v.isOccupied()) throw new InvalidPlacementException("Vertex already occupied");
        for (Vertex other : gameMap.getVertices().values()) {
            if (other.isOccupied()
                    && gameMap.distanceInVertices(vertexId, other.getId()) < 2) {
                throw new InvalidPlacementException("Must be at least 2 edges from another company");
            }
        }
    }

    private void validateEdgeForSetup(Player player, int edgeId, int anchorVertexId) throws GameException {
        Edge e = gameMap.getEdges().get(edgeId);
        if (e == null) throw new InvalidPlacementException("Unknown edge");
        if (e.hasPartnership()) throw new InvalidPlacementException("Edge already has a Partnership");
        if (!e.connects(anchorVertexId)) {
            throw new InvalidPlacementException("Setup Partnership must touch the placed MVP");
        }
    }


    public synchronized void assignRole(int playerId, FounderRole role) throws Exception
    {
        if (role != FounderRole.NONE)
        {
            for (Player p : players)
            {
                if (p.getRole() == role)
                {
                    throw new IllegalActionException("Role " + role.display() + " is already taken");
                }
            }
        }
        Player player = players.get(playerId);
        player.setRole(role);
        if (role == FounderRole.TECH_GURU)
        {
            player.addResource(Resource.CAPITAL, GameConstants.TECH_GURU_BONUS_CAPITAL);
        }
        log(player.getName() + " selected role " + role.display());
    }

    private void updateLongestNetwork()
    {
        int bestLen = 0;
        Player bestPlayer = null;
        for (Player p : players)
        {
            int len = longestPartnershipChain(p);
            if (len >= GameConstants.LONGEST_NETWORK_MIN)
            {
                if (len > bestLen)
                {
                    bestLen = len;
                    bestPlayer = p;
                }
                else if (len == bestLen && bestPlayer != null
                        && p.getLongestNetworkAchievedAtTurn() < bestPlayer.getLongestNetworkAchievedAtTurn())
                {
                    bestPlayer = p;
                }
            }
        }
        for (Player p : players)
        {
            boolean now = p == bestPlayer;
            if (now && !p.holdsLongestNetwork())
            {
                p.setLongestNetworkAchievedAtTurn(turnCount);
            }
            p.setHoldsLongestNetwork(now);
        }
        if (bestPlayer != null)
        {
            longestNetworkHolder = bestPlayer.getId();
            longestNetworkLength = bestLen;
        }
        else
        {
            longestNetworkHolder = -1;
            longestNetworkLength = 0;
        }
    }

    private int longestPartnershipChain(Player player)
    {
        Set<Integer> ownedEdgeIds = new HashSet<>();
        Set<Integer> endpointVertices = new HashSet<>();
        for (Edge e : player.getOwnedEdges())
        {
            ownedEdgeIds.add(e.getId());
            endpointVertices.add(e.getVertexA());
            endpointVertices.add(e.getVertexB());
        }
        int best = 0;
        for (int start : endpointVertices)
        {
            best = Math.max(best, walkChain(start, ownedEdgeIds, new HashSet<Integer>()));
        }
        return best;
    }

    private int walkChain(int vertexId, Set<Integer> ownedEdgeIds, Set<Integer> usedEdges)
    {
        int best = 0;
        for (Edge e : gameMap.getVertexEdges(vertexId))
        {
            if (!ownedEdgeIds.contains(e.getId())) continue;
            if (usedEdges.contains(e.getId())) continue;
            usedEdges.add(e.getId());
            int sub = 1 + walkChain(e.other(vertexId), ownedEdgeIds, usedEdges);
            usedEdges.remove(e.getId());
            if (sub > best) best = sub;
        }
        return best;
    }


    public synchronized void setupInitialPlacement(int playerId, int vertexId, int edgeId, boolean grantResources) throws GameException
    {
        Player player = players.get(playerId);
        validateVertexForBuild(player, vertexId, true);
        MVP mvp = new MVP(playerId);
        gameMap.getVertices().get(vertexId).setStructure(mvp);
        Vertex vtx = gameMap.getVertices().get(vertexId);
        vtx.setStructure(mvp);
        player.registerStructureVertex(vtx);

        validateEdgeForSetup(player, edgeId, vertexId);
        Partnership partnership = new Partnership(playerId);
        gameMap.getEdges().get(edgeId).setPartnership(partnership);
        Edge edg = gameMap.getEdges().get(edgeId);
        edg.setPartnership(partnership);
        player.registerEdge(edg);

        if (grantResources)
        {
            for (Resource r : gameMap.adjacentResources(vertexId))
            {
                player.addResource(r, 1);
            }
            log(player.getName() + " received starting resources from second MVP");
        }
        updateLongestNetwork();
        log(player.getName() + " placed setup MVP at vertex " + vertexId
                + " and Partnership at edge " + edgeId);
    }

    // ---------------- Build: MVP ----------------
    public synchronized void buildMVP(int playerId, int vertexId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        // placement validity: known, unoccupied, distance-2 rule
        validateVertexForBuild(player, vertexId, false);

        // must be afforded
        if (!player.getBank().canAfford(GameConstants.MVP_COST)) {
            throw new InsufficientResourcesException("Not enough resources to build an MVP");
        }

        player.getBank().pay(GameConstants.MVP_COST);
        Vertex vtx = gameMap.getVertices().get(vertexId);
        vtx.setStructure(new MVP(playerId));
        player.registerStructureVertex(vtx);

        log(player.getName() + " built an MVP at vertex " + vertexId);
        checkVictory(player);
    }

    // ---------------- Upgrade: MVP -> Unicorn ----------------
    public synchronized void upgradeToUnicorn(int playerId, int vertexId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        Vertex vtx = gameMap.getVertices().get(vertexId);
        if (vtx == null) throw new InvalidPlacementException("Unknown vertex");

        CompanyStructure cs = vtx.getStructure();
        if (!(cs instanceof MVP)) {
            throw new IllegalActionException("Only an existing MVP can be upgraded to a Unicorn");
        }
        if (cs.getOwnerId() != playerId) {
            throw new IllegalActionException("Cannot upgrade an MVP you do not own");
        }

        // CEO Hacker pays 1 less Cloud
        Map<Resource, Integer> cost = new EnumMap<>(GameConstants.UNICORN_COST);
        if (player.getRole() == FounderRole.CEO_HACKER) {
            int cloud = cost.getOrDefault(Resource.CLOUD, 0)
                    - GameConstants.HACKER_UNICORN_CLOUD_DISCOUNT;
            cost.put(Resource.CLOUD, Math.max(0, cloud));
        }

        if (!player.getBank().canAfford(cost)) {
            throw new InsufficientResourcesException("Not enough resources to upgrade to a Unicorn");
        }

        player.getBank().pay(cost);
        vtx.setStructure(new Unicorn(playerId));
        // vertex already registered when MVP was built; keep registration idempotent
        player.registerStructureVertex(vtx);

        log(player.getName() + " upgraded an MVP to a Unicorn at vertex " + vertexId);
        checkVictory(player);
    }

    // ---------------- Build: Partnership ----------------
    public synchronized void buildPartnership(int playerId, int edgeId) throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        Player player = players.get(playerId);

        Edge edge = gameMap.getEdges().get(edgeId);
        if (edge == null) throw new InvalidPlacementException("Unknown edge");
        if (edge.hasPartnership()) {
            throw new InvalidPlacementException("Edge already has a Partnership");
        }

        // connection rule: must touch a vertex the player already controls
        // (an owned structure vertex, or an endpoint of an owned Partnership edge)
        if (!isConnectedToPlayer(player, edge)) {
            throw new InvalidPlacementException(
                    "Partnership must connect to your existing network");
        }

        if (!player.getBank().canAfford(GameConstants.PARTNERSHIP_COST)) {
            throw new InsufficientResourcesException("Not enough resources to build a Partnership");
        }

        player.getBank().pay(GameConstants.PARTNERSHIP_COST);
        edge.setPartnership(new Partnership(playerId));
        player.registerEdge(edge);

        updateLongestNetwork();
        log(player.getName() + " built a Partnership at edge " + edgeId);
        checkVictory(player);
    }

    private boolean isConnectedToPlayer(Player player, Edge edge) {
        int a = edge.getVertexA();
        int b = edge.getVertexB();
        // touches an owned structure
        for (Vertex v : player.getOwnedVertices()) {
            if (v.getId() == a || v.getId() == b) return true;
        }
        // touches an endpoint of an owned Partnership edge
        for (Edge owned : player.getOwnedEdges()) {
            if (owned.connects(a) || owned.connects(b)) return true;
        }
        return false;
    }

    // ---------------- Trade with the Market ----------------
    public synchronized void tradeWithMarket(int playerId, Resource give, Resource receive)
            throws GameException {
        if (gameOver) throw new IllegalActionException("Game is already over");
        if (give == receive) {
            throw new IllegalTradeException("Cannot trade a resource for itself");
        }
        Player player = players.get(playerId);

        int ratio = player.getRole().marketTradeRatio(); // 4:1 default, 3:1 for CEO Hacker
        if (!player.getBank().has(give, ratio)) {
            throw new InsufficientResourcesException(
                    "Need " + ratio + " " + give.display() + " to trade for 1 " + receive.display());
        }

        player.removeResource(give, ratio);
        player.addResource(receive, 1);
        market.onBuy(receive);

        log(player.getName() + " traded " + ratio + " " + give.display()
                + " for 1 " + receive.display());
    }


    public synchronized int rollDice() throws GameException
    {
        if (gameOver)
        {
            throw new IllegalActionException("Game is already over");
        }
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        lastDiceRoll = d1 + d2;
        log(currentPlayer().getName() + " rolled " + d1 + " + " + d2 + " = " + lastDiceRoll);
        if (lastDiceRoll == GameConstants.CRISIS_DICE_SUM)
        {
            triggerCrisisTax();
        }
        else
        {
            produceResources(lastDiceRoll);
        }
        market.onTurnElapsed();
        return lastDiceRoll;
    }

    private void produceResources(int diceSum) {
        for (Sector sector : gameMap.sectorsWithActivation(diceSum)) {
            if (sector == inspectorSector) {
                log("Sector blocked by inspector, no production");
                continue;
            }
            Resource produced = sector.getType().produces();
            if (produced == null) continue;
            for (Vertex v : gameMap.verticesAround(sector)) {
                CompanyStructure cs = v.getStructure();
                if (cs != null) {
                    Map<Resource, Integer> out = cs.produce(produced);
                    Player owner = players.get(cs.getOwnerId());
                    owner.grantProduction(out);
                    Integer amt = out.get(produced);
                    if (amt != null) {
                        log(owner.getName() + " gained " + amt + " " + produced.display());
                    }
                }
            }
        }
    }


    private void triggerCrisisTax()
    {
        log("Regulatory crisis! Dice sum is 7");
        for (Player p : players)
        {
            int limit = p.getRole().crisisCardLimit();
            int total = p.totalCards();
            if (total > limit)
            {
                int toReturn = total/GameConstants.CRISIS_DISCARD_DIVISOR;
                p.markPendingTax(toReturn);
                log(p.getName() + " must return " + toReturn + " cards (has " + total + ")");
            }
        }
    }

    public synchronized void applyCrisisTax(int playerId, java.util.Map<Resource, Integer> returned) throws Exception
    {
        Player player = players.get(playerId);
        int required = player.getPendingTax();
        int sum = 0;
        for (int v : returned.values())
        {
            sum += v;
        }
        if (sum != required)
        {
            throw new IllegalActionException("Must return exactly " + required + " cards");
        }
        for (java.util.Map.Entry<Resource, Integer> e : returned.entrySet())
        {
            if (player.getResource(e.getKey()) < e.getValue())
            {
                throw new InsufficientResourcesException("Cannot return more cards than held");
            }
        }
        for (java.util.Map.Entry<Resource, Integer> e : returned.entrySet())
        {
            player.removeResource(e.getKey(), e.getValue());
        }
        player.clearPendingTax();
        log(player.getName() + " returned " + required + " cards to the bank");
    }

    public synchronized void placeInspector(int playerId, int row, int col) throws Exception {
        if (lastDiceRoll != GameConstants.CRISIS_DICE_SUM) {
            throw new IllegalActionException("Inspector can only move during a crisis");
        }
        Sector target = gameMap.getSectors()[row][col];
        boolean anyOccupied = gameMap.anySectorOccupied();
        if (anyOccupied && !gameMap.sectorHasCompany(target)) {
            throw new InvalidPlacementException("Inspector must go on an occupied sector");
        }
        inspectorSector = target;
        target.setInspected(true);
        log(players.get(playerId).getName() + " placed the inspector");
    }


    private void checkVictory(Player player) {
        if (player.computeScore() >= GameConstants.VICTORY_POINTS) {
            gameOver = true;
            winner = player;
            log(player.getName() + " has won with " + player.computeScore() + " points!");
        }
    }

    // ---------------- Turn Management ----------------

    /**
     * Ends the current player's turn. 
     * According to page 11, victory is checked at the end of the turn.
     */
    public synchronized void endTurn() {
        if (gameOver) return;

        Player player = currentPlayer();
        
        // Final victory check before passing the turn
        if (player.computeScore() >= GameConstants.VICTORY_POINTS) {
            gameOver = true;
            winner = player;
            log(player.getName() + " wins the game with " + player.computeScore() + " points!");
            return;
        }

        // Advance to next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        
        // If we returned to the first player, a full round (turn) has passed
        if (currentPlayerIndex == 0) {
            turnCount++;
        }
        
        log("It is now " + currentPlayer().getName() + "'s turn.");
    }

    // ---------------- Persistence (Save/Load) ----------------

    /**
     * Saves the entire game state to a file using Java Serialization.
     * Required for the "Persistence" requirement (Page 12).
     */
    public void saveGame(String filePath) throws GameException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(this);
            log("Game saved successfully to " + filePath);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save game: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a GameController instance from a file.
     */
    public static GameController loadGame(String filePath) throws GameException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (GameController) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Failed to load game: " + e.getMessage(), e);
        }
    }

    // ---------------- Getters for UI/Integration ----------------

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public GameMap getGameMap() { return gameMap; }
    public Market getMarket() { return market; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public int getTurnCount() { return turnCount; }
    public boolean isGameOver() { return gameOver; }
    public Player getWinner() { return winner; }
    public List<String> getEventLog() { return Collections.unmodifiableList(eventLog); }
    public Sector getInspectorSector() { return inspectorSector; }
    public int getLastDiceRoll() { return lastDiceRoll; }


    private Player currentPlayer()
    {
        return players.get(currentPlayerIndex);
    }

    private void log(String message)
    {
        eventLog.add("[Turn " + turnCount + "] " + message);
    }
}


public class Main extends Application
{
    private GameController game;
    private Thread gameThread;
    private Stage primaryStage;
    
    // UI Components
    private GridPane mapGrid;
    private VBox rightPanel;
    private TextArea eventLogArea;
    private Label currentPlayerLabel;
    private Label marketPricesLabel;
    private VBox playersResourcesBox;
    
    // Control buttons
    private Button rollButton;
    private Button endTurnButton;
    private Button buildMVPButton;
    private Button upgradeButton;
    private Button buildPartnershipButton;
    private Button marketButton;
    
    private boolean waitingForCrisisPayment = false;
    private int currentCrisisPlayerIndex = 0;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showMainMenu();
    }
    
    private void showMainMenu() {
        VBox menuBox = new VBox(15);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 40px;");
        
        Text title = new Text("Silicon Valley: The Tech Cartel");
        title.setStyle("-fx-font-size: 28px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        Button newGameBtn = new Button("New Game");
        newGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 200px;");
        newGameBtn.setOnAction(e -> showPlayerSetup());
        
        Button loadGameBtn = new Button("Load Game");
        loadGameBtn.setStyle("-fx-font-size: 18px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 200px;");
        loadGameBtn.setOnAction(e -> loadGame());
        
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
        setupBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");
        
        Text title = new Text("Game Setup");
        title.setStyle("-fx-font-size: 24px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        HBox playerCountBox = new HBox(10);
        playerCountBox.setAlignment(Pos.CENTER);
        Label countLabel = new Label("Number of Players:");
        countLabel.setStyle("-fx-fill: #eeeeee;");
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
                playerLabel.setStyle("-fx-fill: #eeeeee;");
                TextField tf = new TextField("Player " + (i + 1));
                nameBox.getChildren().addAll(playerLabel, tf);
            }
        };
        
        playerCountCombo.setOnAction(e -> updateNameFields.run());
        updateNameFields.run();
        
        // Role selection checkbox
        CheckBox roleToggle = new CheckBox("Enable Founder Roles (costs 1 point)");
        roleToggle.setStyle("-fx-text-fill: #eeeeee;");
        
        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> {
            List<String> names = new ArrayList<>();
            int playerCount = playerCountCombo.getValue();
            for (int i = 0; i < playerCount; i++) {
                TextField tf = (TextField) nameBox.getChildren().get(i * 2 + 1);
                names.add(tf.getText());
            }
            startNewGame(names, roleToggle.isSelected());
        });
        
        setupBox.getChildren().addAll(title, playerCountBox, nameBox, roleToggle, startButton);
        
        Scene scene = new Scene(setupBox, 800, 600);
        primaryStage.setScene(scene);
    }
    
    private void startNewGame(List<String> playerNames, boolean withRoles) {
        // Run game logic in separate thread (Thread management requirement)
        gameThread = new Thread(() -> {
            try {
                long seed = System.currentTimeMillis();
                game = new GameController(playerNames, seed);
                
                // Run initial placement
                Platform.runLater(() -> {
                    try {
                        performInitialPlacement();
                        if (withRoles) {
                            showRoleSelection();
                        } else {
                            showGameUI();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showError("Initial placement failed: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> showError("Failed to create game: " + ex.getMessage()));
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }
    
    private void performInitialPlacement() throws Exception {
        // Round 1: forward order
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            // Find a valid vertex
            Vertex vertex = findValidVertexForPlacement();
            if (vertex != null) {
                // Find an adjacent edge
                Edge edge = findAdjacentEdge(vertex.getId());
                if (edge != null) {
                    game.setupInitialPlacement(player.getId(), vertex.getId(), edge.getId(), false);
                }
            }
        }
        
        // Round 2: reverse order with resource grant
        for (int i = game.getPlayers().size() - 1; i >= 0; i--) {
            Player player = game.getPlayers().get(i);
            Vertex vertex = findValidVertexForPlacement();
            if (vertex != null) {
                Edge edge = findAdjacentEdge(vertex.getId());
                if (edge != null) {
                    game.setupInitialPlacement(player.getId(), vertex.getId(), edge.getId(), true);
                }
            }
        }
    }
    
    private Vertex findValidVertexForPlacement() {
        for (Vertex v : game.getGameMap().getVertices().values()) {
            if (!v.isOccupied()) {
                boolean valid = true;
                for (Vertex other : game.getGameMap().getVertices().values()) {
                    if (other.isOccupied() && game.getGameMap().distanceInVertices(v.getId(), other.getId()) < 2) {
                        valid = false;
                        break;
                    }
                }
                if (valid) return v;
            }
        }
        return null;
    }
    
    private Edge findAdjacentEdge(int vertexId) {
        for (Edge e : game.getGameMap().getEdges().values()) {
            if (e.connects(vertexId) && !e.hasPartnership()) {
                return e;
            }
        }
        return null;
    }
    
    private void showRoleSelection() {
        Stage roleStage = new Stage();
        VBox roleBox = new VBox(10);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setStyle("-fx-background-color: #1a1a2e; -fx-padding: 20px;");
        
        Text title = new Text("Select Founder Roles (Optional)");
        title.setStyle("-fx-font-size: 20px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        Label infoLabel = new Label("Each role can only be taken by ONE player. Roles cost -1 point.");
        infoLabel.setStyle("-fx-fill: #ff9800; -fx-font-size: 12px;");
        
        List<Player> players = game.getPlayers();
        Map<Integer, FounderRole> selectedRoles = new HashMap<>();
        
        // Track available roles
        List<FounderRole> availableRoles = new ArrayList<>(Arrays.asList(
            FounderRole.CEO_HACKER, FounderRole.TECH_GURU, FounderRole.VC_FUNDED
        ));
        
        // Store combo boxes for later updates
        List<ComboBox<String>> roleComboBoxes = new ArrayList<>();
        List<TextArea> descAreas = new ArrayList<>();
        List<Player> playerList = players;
        
        for (int idx = 0; idx < players.size(); idx++) {
            Player player = players.get(idx);
            VBox playerBox = new VBox(5);
            playerBox.setStyle("-fx-padding: 10px; -fx-border-color: #444; -fx-border-radius: 5px; -fx-background-color: #0f3460;");
            
            Label nameLabel = new Label(player.getName());
            nameLabel.setStyle("-fx-fill: #ffd700; -fx-font-weight: bold; -fx-font-size: 14px;");
            
            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.getItems().add("No Role");
            for (FounderRole r : availableRoles) {
                roleCombo.getItems().add(r.display());
            }
            roleCombo.setValue("No Role");
            roleCombo.setStyle("-fx-font-size: 12px; -fx-min-width: 180px;");
            
            TextArea descArea = new TextArea();
            descArea.setEditable(false);
            descArea.setPrefHeight(50);
            descArea.setStyle("-fx-control-inner-background: #1a1a2e; -fx-text-fill: #cccccc;");
            descArea.setText("No special abilities");
            
            roleComboBoxes.add(roleCombo);
            descAreas.add(descArea);
            
            roleCombo.setOnAction(e -> {
                String selected = roleCombo.getValue();
                
                // If previously had a role, add it back to available
                FounderRole oldRole = selectedRoles.get(player.getId());
                if (oldRole != null && oldRole != FounderRole.NONE) {
                    availableRoles.add(oldRole);
                }
                
                if (selected.equals("No Role")) {
                    selectedRoles.put(player.getId(), FounderRole.NONE);
                    descArea.setText("No special abilities");
                } else {
                    // Find which role was selected
                    FounderRole newRole = null;
                    for (FounderRole r : FounderRole.values()) {
                        if (r.display().equals(selected)) {
                            newRole = r;
                            break;
                        }
                    }
                    
                    if (newRole != null && availableRoles.contains(newRole)) {
                        availableRoles.remove(newRole);
                        selectedRoles.put(player.getId(), newRole);
                        
                        // Update description
                        switch (newRole) {
                            case CEO_HACKER:
                                descArea.setText("Trades at 3:1 rate instead of 4:1\nUnicorn upgrade costs 1 less Cloud");
                                break;
                            case TECH_GURU:
                                descArea.setText("Starts with +2 Capital\nHolds 9 cards before tax (instead of 7)");
                                break;
                            case VC_FUNDED:
                                descArea.setText("+2 starting Capital\nCard limit increased to 9 during crisis");
                                break;
                            default:
                                descArea.setText("No special abilities");
                        }
                    } else {
                        // Role not available - revert
                        showError("This role is already taken by another player!");
                        roleCombo.setValue("No Role");
                        selectedRoles.put(player.getId(), FounderRole.NONE);
                        descArea.setText("No special abilities");
                    }
                }
                
                // Update all combo boxes to show only available roles
                for (int i = 0; i < playerList.size(); i++) {
                    Player p = playerList.get(i);
                    ComboBox<String> cb = roleComboBoxes.get(i);
                    
                    cb.getItems().clear();
                    cb.getItems().add("No Role");
                    
                    // Add available roles
                    for (FounderRole r : availableRoles) {
                        cb.getItems().add(r.display());
                    }
                    
                    // Add the player's currently selected role if they have one
                    FounderRole playerRole = selectedRoles.get(p.getId());
                    if (playerRole != null && playerRole != FounderRole.NONE && !availableRoles.contains(playerRole)) {
                        cb.getItems().add(playerRole.display());
                    }
                    
                    // Restore selection
                    if (playerRole != null && playerRole != FounderRole.NONE) {
                        cb.setValue(playerRole.display());
                    } else {
                        cb.setValue("No Role");
                    }
                }
            });
            
            playerBox.getChildren().addAll(nameLabel, roleCombo, descArea);
            roleBox.getChildren().add(playerBox);
            selectedRoles.put(player.getId(), FounderRole.NONE);
        }
        
        Button confirmBtn = new Button("Start Game");
        confirmBtn.setStyle("-fx-font-size: 16px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 150px; -fx-padding: 10px;");
        confirmBtn.setOnAction(e -> {
            // Apply selected roles
            for (Map.Entry<Integer, FounderRole> entry : selectedRoles.entrySet()) {
                if (entry.getValue() != null && entry.getValue() != FounderRole.NONE) {
                    try {
                        game.assignRole(entry.getKey(), entry.getValue());
                    } catch (Exception ex) {
                        showError("Failed to assign role: " + ex.getMessage());
                    }
                }
            }
            roleStage.close();
            showGameUI();
        });
        
        roleBox.getChildren().add(confirmBtn);
        
        ScrollPane scrollPane = new ScrollPane(roleBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1a1a2e; -fx-background-color: #1a1a2e;");
        
        Scene scene = new Scene(scrollPane, 550, 550);
        roleStage.setScene(scene);
        roleStage.setTitle("Select Founder Roles");
        roleStage.show();
    }
    
    private void showGameUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        
        // Menu bar
        MenuBar menuBar = new MenuBar();
        Menu gameMenu = new Menu("Game");
        MenuItem saveItem = new MenuItem("Save Game");
        saveItem.setOnAction(e -> saveGame());
        MenuItem loadItem = new MenuItem("Load Game");
        loadItem.setOnAction(e -> loadGame());
        MenuItem exitItem = new MenuItem("Exit to Menu");
        exitItem.setOnAction(e -> showMainMenu());
        gameMenu.getItems().addAll(saveItem, loadItem, exitItem);
        
        Menu helpMenu = new Menu("Help");
        MenuItem rulesItem = new MenuItem("Game Rules");
        rulesItem.setOnAction(e -> showRules());
        helpMenu.getItems().add(rulesItem);
        
        menuBar.getMenus().addAll(gameMenu, helpMenu);
        root.setTop(menuBar);
        
        // Map grid
        mapGrid = new GridPane();
        mapGrid.setHgap(5);
        mapGrid.setVgap(5);
        mapGrid.setAlignment(Pos.CENTER);
        mapGrid.setPadding(new javafx.geometry.Insets(20));
        updateMapGrid();
        
        // Right panel
        rightPanel = new VBox(10);
        rightPanel.setStyle("-fx-background-color: #16213e; -fx-padding: 15px;");
        rightPanel.setPrefWidth(350);
        
        currentPlayerLabel = new Label("Current: " + game.getPlayers().get(game.getCurrentPlayerIndex()).getName());
        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-fill: #ffd700; -fx-font-weight: bold;");
        
        marketPricesLabel = new Label();
        updateMarketDisplay();
        
        playersResourcesBox = new VBox(5);
        updateResourcesDisplay();
        
        eventLogArea = new TextArea();
        eventLogArea.setEditable(false);
        eventLogArea.setPrefHeight(200);
        eventLogArea.setStyle("-fx-control-inner-background: #0f3460; -fx-text-fill: #eeeeee;");
        updateEventLog();
        
        // Buttons
        rollButton = new Button("🎲 Roll Dice");
        rollButton.setStyle("-fx-font-size: 14px; -fx-background-color: #2196F3; -fx-text-fill: white; -fx-min-width: 150px;");
        rollButton.setOnAction(e -> rollDice());
        
        buildMVPButton = new Button("🏗️ Build MVP");
        buildMVPButton.setStyle("-fx-font-size: 14px; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-min-width: 150px;");
        buildMVPButton.setOnAction(e -> showBuildMVPMenu());
        buildMVPButton.setDisable(true);
        
        upgradeButton = new Button("🦄 Upgrade to Unicorn");
        upgradeButton.setStyle("-fx-font-size: 14px; -fx-background-color: #FF9800; -fx-text-fill: white; -fx-min-width: 150px;");
        upgradeButton.setOnAction(e -> showUpgradeMenu());
        upgradeButton.setDisable(true);
        
        buildPartnershipButton = new Button("🤝 Build Partnership");
        buildPartnershipButton.setStyle("-fx-font-size: 14px; -fx-background-color: #9C27B0; -fx-text-fill: white; -fx-min-width: 150px;");
        buildPartnershipButton.setOnAction(e -> showBuildPartnershipMenu());
        buildPartnershipButton.setDisable(true);
        
        marketButton = new Button("💰 Market");
        marketButton.setStyle("-fx-font-size: 14px; -fx-background-color: #009688; -fx-text-fill: white; -fx-min-width: 150px;");
        marketButton.setOnAction(e -> showMarketMenu());
        marketButton.setDisable(true);
        
        endTurnButton = new Button("⏭️ End Turn");
        endTurnButton.setStyle("-fx-font-size: 14px; -fx-background-color: #f44336; -fx-text-fill: white; -fx-min-width: 150px;");
        endTurnButton.setOnAction(e -> endTurn());
        endTurnButton.setDisable(true);
        
        HBox buttonBox = new HBox(10, rollButton, buildMVPButton, upgradeButton, buildPartnershipButton, marketButton, endTurnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        rightPanel.getChildren().addAll(
            currentPlayerLabel, 
            new Separator(),
            marketPricesLabel,
            new Separator(),
            new Label("Player Resources:"),
            playersResourcesBox,
            new Separator(),
            new Label("Event Log:"),
            eventLogArea,
            buttonBox
        );
        
        ScrollPane mapScroll = new ScrollPane(mapGrid);
        mapScroll.setFitToWidth(true);
        mapScroll.setFitToHeight(true);
        mapScroll.setStyle("-fx-background: #1a1a2e;");
        
        root.setCenter(mapScroll);
        root.setRight(rightPanel);
        
        Scene scene = new Scene(root, 1400, 900);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Silicon Valley: The Tech Cartel");
        primaryStage.show();
        
        // Start auto-refresh for event log
        startEventLogUpdater();
    }
    
    private void startEventLogUpdater() {
        Thread updater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        updateEventLog();
                        updateResourcesDisplay();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updater.setDaemon(true);
        updater.start();
    }
    
    private void updateMapGrid() {
        mapGrid.getChildren().clear();
        GameMap gameMap = game.getGameMap();
        Sector[][] sectors = gameMap.getSectors();
        
        for (int row = 0; row < sectors.length; row++) {
            for (int col = 0; col < sectors[0].length; col++) {
                Sector sector = sectors[row][col];
                StackPane cell = createSectorCell(sector, row, col);
                mapGrid.add(cell, col, row);
            }
        }
    }
    
    private StackPane createSectorCell(Sector sector, int row, int col) {
        StackPane cell = new StackPane();
        cell.setPrefSize(100, 100);
        cell.setStyle("-fx-border-color: #333; -fx-border-width: 1px; -fx-border-radius: 5px;");
        
        Color bgColor = getSectorColor(sector.getType());
        Rectangle bg = new Rectangle(100, 100);
        bg.setFill(bgColor);
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        
        // Inspector indicator
        if (game.getInspectorSector() == sector) {
            Rectangle inspectorBadge = new Rectangle(25, 25);
            inspectorBadge.setFill(Color.RED);
            inspectorBadge.setArcWidth(5);
            inspectorBadge.setArcHeight(5);
            Text inspectorText = new Text("🔍");
            inspectorText.setStyle("-fx-font-size: 12px;");
            StackPane inspectorPane = new StackPane(inspectorBadge, inspectorText);
            StackPane.setAlignment(inspectorPane, Pos.TOP_RIGHT);
            cell.getChildren().add(inspectorPane);
        }
        
        String displayName = sector.getType().display();
        String shortName = displayName.length() > 8 ? displayName.substring(0, 8) : displayName;
        Text typeText = new Text(shortName);
        typeText.setStyle("-fx-font-size: 9px; -fx-fill: #eeeeee;");
        
        Text activationText = new Text(sector.getType().isProductive() ? String.valueOf(sector.getActivationNumber()) : "⚖");
        activationText.setStyle("-fx-font-size: 18px; -fx-fill: #eeeeee; -fx-font-weight: bold;");
        
        // Show company markers on vertices
        for (Vertex v : game.getGameMap().verticesAround(sector)) {
            if (v.isOccupied()) {
                CompanyStructure cs = v.getStructure();
                String symbol = cs instanceof Unicorn ? "🦄" : "★";
                Text marker = new Text(symbol);
                marker.setStyle("-fx-font-size: 16px; -fx-fill: gold;");
                
                // Position based on vertex position relative to sector
                int vertexId = v.getId();
                if (vertexId % 3 == 0) {
                    StackPane.setAlignment(marker, Pos.TOP_LEFT);
                } else if (vertexId % 3 == 1) {
                    StackPane.setAlignment(marker, Pos.TOP_RIGHT);
                } else if (vertexId % 3 == 2) {
                    StackPane.setAlignment(marker, Pos.BOTTOM_LEFT);
                } else {
                    StackPane.setAlignment(marker, Pos.BOTTOM_RIGHT);
                }
                cell.getChildren().add(marker);
            }
        }
        
        VBox labels = new VBox(5, activationText, typeText);
        labels.setAlignment(Pos.CENTER);
        
        cell.getChildren().addAll(bg, labels);
        return cell;
    }
    
    private Color getSectorColor(SectorType type) {
        switch (type) {
            case HUB_AI: return Color.web("#443199");
            case DISTRICT_FINTECH: return Color.web("#111844");
            case CAMPUS_CLOUD: return Color.web("#C13383");
            case QUARTER_IP: return Color.web("#792CA2");
            case VALLEY_DATA: return Color.web("#E05454");
            case ZONE_REGULATORY: return Color.web("#555555");
            default: return Color.GRAY;
        }
    }
    
    private void updateMarketDisplay() {
        StringBuilder sb = new StringBuilder("💰 Market Prices:\n");
        for (Resource r : Resource.values()) {
            sb.append("  ").append(r.display()).append(": ").append(game.getMarket().priceOf(r)).append("\n");
        }
        marketPricesLabel.setText(sb.toString());
        marketPricesLabel.setStyle("-fx-fill: #eeeeee;");
    }
    
    private void updateResourcesDisplay() {
        playersResourcesBox.getChildren().clear();
        for (Player player : game.getPlayers()) {
            VBox playerBox = new VBox(3);
            playerBox.setStyle("-fx-background-color: #0f3460; -fx-padding: 5px; -fx-border-radius: 5px;");
            
            boolean isCurrent = player.getId() == game.getPlayers().get(game.getCurrentPlayerIndex()).getId();
            String style = isCurrent ? "-fx-fill: #ffd700; -fx-font-weight: bold;" : "-fx-fill: #eeeeee;";
            
            Label nameLabel = new Label(player.getName() + " (Score: " + player.computeScore() + ")");
            nameLabel.setStyle(style);
            
            HBox resourcesBox = new HBox(5);
            for (Resource r : Resource.values()) {
                int amount = player.getResource(r);
                if (amount > 0) {
                    String shortName = r.display().length() > 3 ? r.display().substring(0, 3) : r.display();
                    Label resLabel = new Label(shortName + ":" + amount);
                    resLabel.setStyle("-fx-background-color: " + getResourceColor(r) + "; -fx-padding: 2px 5px; -fx-border-radius: 3px; -fx-text-fill: white; -fx-font-size: 10px;");
                    resourcesBox.getChildren().add(resLabel);
                }
            }
            
            Label totalLabel = new Label("Total: " + player.totalCards());
            totalLabel.setStyle("-fx-fill: #ff9800; -fx-font-size: 10px;");
            
            playerBox.getChildren().addAll(nameLabel, resourcesBox, totalLabel);
            playersResourcesBox.getChildren().add(playerBox);
        }
    }
    
    private String getResourceColor(Resource r) {
        switch (r) {
            case TALENT: return "#443199";
            case CAPITAL: return "#111844";
            case CLOUD: return "#C13383";
            case DATA: return "#E05454";
            case PATENT: return "#792CA2";
            default: return "#555";
        }
    }
    
    private void updateEventLog() {
        StringBuilder sb = new StringBuilder();
        List<String> log = game.getEventLog();
        int start = Math.max(0, log.size() - 20);
        for (int i = start; i < log.size(); i++) {
            sb.append("• ").append(log.get(i)).append("\n");
        }
        eventLogArea.setText(sb.toString());
    }
    
    private void rollDice() {
        try {
            int roll = game.rollDice();
            updateMapGrid();
            updateMarketDisplay();
            updateResourcesDisplay();
            updateEventLog();
            
            // Check if crisis requires tax payment
            if (roll == GameConstants.CRISIS_DICE_SUM) {
                handleCrisisTax();
            } else {
                // Enable action buttons after roll
                enableActionButtons(true);
                rollButton.setDisable(true);
            }
            
        } catch (Exception ex) {
            showError("Roll failed: " + ex.getMessage());
        }
    }
    
    private void handleCrisisTax() {
        waitingForCrisisPayment = true;
        currentCrisisPlayerIndex = 0;
        processNextCrisisPayment();
    }
    
    private void processNextCrisisPayment() {
        if (currentCrisisPlayerIndex >= game.getPlayers().size()) {
            // All taxes paid, now place inspector
            waitingForCrisisPayment = false;
            showInspectorPlacement();
            return;
        }
        
        Player player = game.getPlayers().get(currentCrisisPlayerIndex);
        int pendingTax = player.getPendingTax();
        
        if (pendingTax > 0) {
            showTaxPaymentDialog(player, pendingTax);
        } else {
            currentCrisisPlayerIndex++;
            processNextCrisisPayment();
        }
    }
    
    private void showTaxPaymentDialog(Player player, int amount) {
        Stage taxStage = new Stage();
        VBox taxBox = new VBox(10);
        taxBox.setAlignment(Pos.CENTER);
        taxBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Label title = new Label(player.getName() + " - Pay Tax");
        title.setStyle("-fx-fill: #ffd700; -fx-font-size: 18px;");
        
        Label info = new Label("You must return " + amount + " cards to the bank.");
        info.setStyle("-fx-fill: #eeeeee;");
        
        // Create spinners for each resource type
        Map<Resource, Integer> toReturn = new EnumMap<>(Resource.class);
        VBox resourcesBox = new VBox(5);
        
        for (Resource r : Resource.values()) {
            int available = player.getResource(r);
            if (available > 0) {
                HBox row = new HBox(10);
                Label resLabel = new Label(r.display() + " (" + available + "):");
                resLabel.setStyle("-fx-fill: #eeeeee;");
                Spinner<Integer> spinner = new Spinner<>(0, available, 0);
                spinner.setEditable(true);
                row.getChildren().addAll(resLabel, spinner);
                resourcesBox.getChildren().add(row);
                
                final Resource resource = r;
                spinner.valueProperty().addListener((obs, old, val) -> {
                    toReturn.put(resource, val);
                });
            }
        }
        
        Button confirmBtn = new Button("Pay Tax");
        confirmBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        confirmBtn.setOnAction(e -> {
            int total = toReturn.values().stream().mapToInt(Integer::intValue).sum();
            if (total == amount) {
                try {
                    game.applyCrisisTax(player.getId(), toReturn);
                    taxStage.close();
                    currentCrisisPlayerIndex++;
                    processNextCrisisPayment();
                    updateResourcesDisplay();
                    updateEventLog();
                } catch (Exception ex) {
                    showError("Failed to pay tax: " + ex.getMessage());
                }
            } else {
                showError("Must return exactly " + amount + " cards");
            }
        });
        
        taxBox.getChildren().addAll(title, info, resourcesBox, confirmBtn);
        Scene scene = new Scene(taxBox, 400, 400);
        taxStage.setScene(scene);
        taxStage.showAndWait();
    }
    
    private void showInspectorPlacement() {
        Stage inspectorStage = new Stage();
        VBox inspectorBox = new VBox(10);
        inspectorBox.setAlignment(Pos.CENTER);
        inspectorBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Label title = new Label("Place the Inspector");
        title.setStyle("-fx-fill: #ffd700; -fx-font-size: 18px;");
        
        Label info = new Label("Click on a sector to place the inspector.");
        info.setStyle("-fx-fill: #eeeeee;");
        
        // Create a mini-map for inspector placement
        GridPane miniMap = new GridPane();
        miniMap.setHgap(2);
        miniMap.setVgap(2);
        
        Sector[][] sectors = game.getGameMap().getSectors();
        for (int row = 0; row < sectors.length; row++) {
            for (int col = 0; col < sectors[0].length; col++) {
                Sector sector = sectors[row][col];
                StackPane cell = new StackPane();
                cell.setPrefSize(50, 50);
                cell.setStyle("-fx-border-color: #333; -fx-border-width: 1px;");
                
                Color bgColor = getSectorColor(sector.getType());
                Rectangle bg = new Rectangle(50, 50);
                bg.setFill(bgColor);
                
                Text symbol = new Text(sector.getType().isProductive() ? String.valueOf(sector.getActivationNumber()) : "⚖");
                symbol.setStyle("-fx-fill: white;");
                
                cell.getChildren().addAll(bg, symbol);
                
                final int finalRow = row;
                final int finalCol = col;
                cell.setOnMouseClicked(e -> {
                    try {
                        game.placeInspector(game.getCurrentPlayerIndex(), finalRow, finalCol);
                        inspectorStage.close();
                        updateMapGrid();
                        updateEventLog();
                        enableActionButtons(true);
                        rollButton.setDisable(true);
                    } catch (Exception ex) {
                        showError("Cannot place inspector: " + ex.getMessage());
                    }
                });
                
                miniMap.add(cell, col, row);
            }
        }
        
        inspectorBox.getChildren().addAll(title, info, miniMap);
        Scene scene = new Scene(inspectorBox, 400, 500);
        inspectorStage.setScene(scene);
        inspectorStage.show();
    }
    
    private void showBuildMVPMenu() {
        List<Vertex> availableVertices = new ArrayList<>();
        for (Vertex v : game.getGameMap().getVertices().values()) {
            if (!v.isOccupied()) {
                boolean valid = true;
                for (Vertex other : game.getGameMap().getVertices().values()) {
                    if (other.isOccupied() && game.getGameMap().distanceInVertices(v.getId(), other.getId()) < 2) {
                        valid = false;
                        break;
                    }
                }
                if (valid) availableVertices.add(v);
            }
        }
        
        if (availableVertices.isEmpty()) {
            showError("No valid location for MVP!");
            return;
        }
        
        // Create readable labels for vertices
        Map<String, Vertex> vertexLabels = new LinkedHashMap<>();
        for (Vertex v : availableVertices) {
            // Find coordinates of this vertex (around which sectors)
            String coords = getVertexCoordinates(v);
            vertexLabels.put("Vertex " + v.getId() + " " + coords, v);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(vertexLabels.keySet().iterator().next(), vertexLabels.keySet());
        dialog.setTitle("Build MVP");
        dialog.setHeaderText("Select a vertex location\nCost: 1 Capital, 1 Talent, 1 Cloud, 1 Data");
        dialog.setContentText("Choose vertex:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Vertex vertex = vertexLabels.get(selected);
            try {
                game.buildMVP(game.getCurrentPlayerIndex(), vertex.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("MVP built successfully at " + selected);
            } catch (Exception ex) {
                showError("Cannot build MVP: " + ex.getMessage());
            }
        });
    }

    private void showUpgradeMenu() {
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        List<Vertex> upgradableVertices = new ArrayList<>();
        
        for (Vertex v : current.getOwnedVertices()) {
            if (v.getStructure() instanceof MVP) {
                upgradableVertices.add(v);
            }
        }
        
        if (upgradableVertices.isEmpty()) {
            showError("No MVP to upgrade!");
            return;
        }
        
        // Create readable labels for vertices
        Map<String, Vertex> vertexLabels = new LinkedHashMap<>();
        for (Vertex v : upgradableVertices) {
            String coords = getVertexCoordinates(v);
            vertexLabels.put("MVP at " + coords + " (Vertex " + v.getId() + ")", v);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(vertexLabels.keySet().iterator().next(), vertexLabels.keySet());
        dialog.setTitle("Upgrade to Unicorn");
        dialog.setHeaderText("Select an MVP to upgrade\nCost: 3 Data, 2 Cloud");
        dialog.setContentText("Choose MVP:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Vertex vertex = vertexLabels.get(selected);
            try {
                game.upgradeToUnicorn(game.getCurrentPlayerIndex(), vertex.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("MVP upgraded to Unicorn at " + selected);
            } catch (Exception ex) {
                showError("Cannot upgrade: " + ex.getMessage());
            }
        });
    }

    private void showBuildPartnershipMenu() {
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        List<Edge> availableEdges = new ArrayList<>();
        
        for (Edge e : game.getGameMap().getEdges().values()) {
            if (!e.hasPartnership()) {
                if (isConnectedToPlayer(current, e)) {
                    availableEdges.add(e);
                }
            }
        }
        
        if (availableEdges.isEmpty()) {
            showError("No valid location for Partnership!");
            return;
        }
        
        // Create readable labels for edges
        Map<String, Edge> edgeLabels = new LinkedHashMap<>();
        for (Edge e : availableEdges) {
            String coords = getEdgeCoordinates(e);
            edgeLabels.put("Edge " + e.getId() + " " + coords, e);
        }
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(edgeLabels.keySet().iterator().next(), edgeLabels.keySet());
        dialog.setTitle("Build Partnership");
        dialog.setHeaderText("Select an edge location\nCost: 1 Capital, 1 Patent");
        dialog.setContentText("Choose edge:");
        
        dialog.showAndWait().ifPresent(selected -> {
            Edge edge = edgeLabels.get(selected);
            try {
                game.buildPartnership(game.getCurrentPlayerIndex(), edge.getId());
                updateMapGrid();
                updateResourcesDisplay();
                updateEventLog();
                showInfo("Partnership built successfully at " + selected);
            } catch (Exception ex) {
                showError("Cannot build Partnership: " + ex.getMessage());
            }
        });
    }

    // Helper method to get readable vertex coordinates
    private String getVertexCoordinates(Vertex vertex) {
        // Find the sectors around this vertex to determine position
        List<Sector> adjacent = vertex.getAdjacentSectors();
        if (adjacent.isEmpty()) return "(unknown location)";
        
        // Get min row and col from adjacent sectors
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        for (Sector s : adjacent) {
            minRow = Math.min(minRow, s.getRow());
            minCol = Math.min(minCol, s.getCol());
        }
        return "(between sectors " + minRow + "," + minCol + " and " + (minRow+1) + "," + (minCol+1) + ")";
    }

    // Helper method to get readable edge coordinates
    private String getEdgeCoordinates(Edge edge) {
        int vA = edge.getVertexA();
        int vB = edge.getVertexB();
        
        // Try to find sectors adjacent to this edge
        Vertex vertexA = game.getGameMap().getVertices().get(vA);
        if (vertexA == null) return "(unknown edge)";
        
        List<Sector> sectors = vertexA.getAdjacentSectors();
        if (!sectors.isEmpty()) {
            Sector s = sectors.get(0);
            return "(between sectors at " + s.getRow() + "," + s.getCol() + ")";
        }
        return "(edge between vertices " + vA + " and " + vB + ")";
    }
    
    private boolean isConnectedToPlayer(Player player, Edge edge) {
        int a = edge.getVertexA();
        int b = edge.getVertexB();
        
        for (Vertex v : player.getOwnedVertices()) {
            if (v.getId() == a || v.getId() == b) return true;
        }
        for (Edge e : player.getOwnedEdges()) {
            if (e.connects(a) || e.connects(b)) return true;
        }
        return false;
    }
    
    private void showMarketMenu() {
        Stage marketStage = new Stage();
        VBox marketBox = new VBox(10);
        marketBox.setAlignment(Pos.CENTER);
        marketBox.setStyle("-fx-padding: 20px; -fx-background-color: #1a1a2e;");
        
        Player current = game.getPlayers().get(game.getCurrentPlayerIndex());
        
        Label title = new Label("Dynamic Market");
        title.setStyle("-fx-fill: #ffd700; -fx-font-size: 18px;");
        
        Label capitalLabel = new Label("Your Capital: " + current.getResource(Resource.CAPITAL));
        capitalLabel.setStyle("-fx-fill: #eeeeee;");
        
        for (Resource r : Resource.values()) {
            if (r != Resource.CAPITAL) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER);
                
                int price = game.getMarket().priceOf(r);
                int have = current.getResource(r);
                
                Label info = new Label(r.display() + " - Price: " + price + " (You have: " + have + ")");
                info.setStyle("-fx-fill: #eeeeee;");
                
                Button buyBtn = new Button("Buy");
                buyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                buyBtn.setOnAction(e -> {
                    try {
                        game.tradeWithMarket(game.getCurrentPlayerIndex(), Resource.CAPITAL, r);
                        marketStage.close();
                        updateResourcesDisplay();
                        updateMarketDisplay();
                        updateEventLog();
                    } catch (Exception ex) {
                        showError("Cannot buy: " + ex.getMessage());
                    }
                });
                
                Button sellBtn = new Button("Sell");
                sellBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                sellBtn.setOnAction(e -> {
                    try {
                        game.tradeWithMarket(game.getCurrentPlayerIndex(), r, Resource.CAPITAL);
                        marketStage.close();
                        updateResourcesDisplay();
                        updateMarketDisplay();
                        updateEventLog();
                    } catch (Exception ex) {
                        showError("Cannot sell: " + ex.getMessage());
                    }
                });
                
                row.getChildren().addAll(info, buyBtn, sellBtn);
                marketBox.getChildren().add(row);
            }
        }
        
        marketBox.getChildren().addAll(title, capitalLabel);
        
        Scene scene = new Scene(marketBox, 500, 400);
        marketStage.setScene(scene);
        marketStage.setTitle("Market");
        marketStage.show();
    }
    
    private void enableActionButtons(boolean enabled) {
        buildMVPButton.setDisable(!enabled);
        upgradeButton.setDisable(!enabled);
        buildPartnershipButton.setDisable(!enabled);
        marketButton.setDisable(!enabled);
        endTurnButton.setDisable(!enabled);
    }
    
    private void endTurn() {
        try {
            game.endTurn();
            enableActionButtons(false);
            rollButton.setDisable(false);
            
            currentPlayerLabel.setText("Current: " + game.getPlayers().get(game.getCurrentPlayerIndex()).getName());
            updateResourcesDisplay();
            updateEventLog();
            
            if (game.isGameOver()) {
                showWinner();
            }
        } catch (Exception ex) {
            showError("End turn failed: " + ex.getMessage());
        }
    }
    
    private void saveGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showSaveDialog(primaryStage);
        
        if (file != null) {
            try {
                game.saveGame(file.getAbsolutePath());
                showInfo("Game saved to " + file.getName());
            } catch (GameException ex) {
                showError("Save failed: " + ex.getMessage());
            }
        }
    }
    
    private void loadGame() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Game Files", "*.sav"));
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                game = GameController.loadGame(file.getAbsolutePath());
                showGameUI();
            } catch (GameException ex) {
                showError("Load failed: " + ex.getMessage());
            }
        }
    }
    
    private void showRules() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Rules");
        alert.setHeaderText("Silicon Valley: The Tech Cartel");
        alert.setContentText(
            "🏆 WIN: First to 10 points!\n\n" +
            "⭐ SCORING:\n" +
            "  • MVP: 1 point\n" +
            "  • Unicorn: 2 points\n" +
            "  • Longest Partnership: 2 points\n" +
            "  • Founder Role: -1 point\n\n" +
            "🎲 GAMEPLAY:\n" +
            "  • Roll dice (2-12)\n" +
            "  • Matching sectors produce resources\n" +
            "  • Roll 7 = Crisis (Tax + Inspector)\n\n" +
            "🏗️ BUILD:\n" +
            "  • MVP: 1 each of Capital, Talent, Cloud, Data\n" +
            "  • Unicorn: 3 Data, 2 Cloud\n" +
            "  • Partnership: 1 Capital, 1 Patent"
        );
        alert.showAndWait();
    }
    
    private void showWinner() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("🏆 " + game.getWinner().getName() + " wins! 🏆");
        alert.setContentText("Final Score: " + game.getWinner().computeScore() + " points");
        alert.showAndWait();
        showMainMenu();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}