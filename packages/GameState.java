package packages;

import java.io.*;
import java.util.*;

public class GameState implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private List<PlayerSaveData> players;
    private Map<String, Integer> marketPrices;
    private Map<String, Integer> turnsSinceLastPurchase;
    private int currentPlayerIndex;
    private int currentTurn;
    private Point auditorPosition;
    private List<String> eventLog;
    private SectorSaveData[][] sectors;
    private NodeSaveData[][] nodes;
    
    public static class PlayerSaveData implements Serializable
    {
        public int id;
        public String name;
        public Map<String, Integer> resources;
        public List<CompanySaveData> companies;
        public List<PartnershipSaveData> partnerships;
        public String role;
        
        public PlayerSaveData()
        {
            resources = new HashMap<String, Integer>();
            companies = new ArrayList<CompanySaveData>();
            partnerships = new ArrayList<PartnershipSaveData>();
        }
    }
    
    public static class CompanySaveData implements Serializable
    {
        public String type;
        public int ownerId;
        public int nodeX, nodeY;
    }
    public static class PartnershipSaveData implements Serializable
    {
        public int ownerId;
        public int edgeX1, edgeY1, edgeX2, edgeY2;
    }
    public static class SectorSaveData implements Serializable
    {
        public String type;
        public int diceNumber;
        public boolean hasAuditor;
        public int x, y;
    }
    public static class NodeSaveData implements Serializable
    {
        public int x, y;
        public boolean hasCompany;
        public String companyType;
        public int ownerId;
    }
    
    public List<PlayerSaveData> getPlayers()             { return players; }
    public void setPlayers(List<PlayerSaveData> players) { this.players = players; }
    
    public Map<String, Integer> getMarketPrices()                  { return marketPrices; }
    public void setMarketPrices(Map<String, Integer> marketPrices) { this.marketPrices = marketPrices; }

    public Map<String, Integer> getTurnsSinceLastPurchase()                            { return turnsSinceLastPurchase; }
    public void setTurnsSinceLastPurchase(Map<String, Integer> turnsSinceLastPurchase) { this.turnsSinceLastPurchase = turnsSinceLastPurchase; }
    

    public int getCurrentPlayerIndex()                        { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }
    
    public int getCurrentTurn()                 { return currentTurn; }
    public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; }
    
    public Point getAuditorPosition()                     { return auditorPosition; }
    public void setAuditorPosition(Point auditorPosition) { this.auditorPosition = auditorPosition; }
    
    
    public List<String> getEventLog()              { return eventLog; }
    public void setEventLog(List<String> eventLog) { this.eventLog = eventLog; }
    
    public SectorSaveData[][] getSectors()             { return sectors; }
    public void setSectors(SectorSaveData[][] sectors) { this.sectors = sectors; }
    
    public NodeSaveData[][] getNodes()           { return nodes; }
    public void setNodes(NodeSaveData[][] nodes) { this.nodes = nodes; }
}