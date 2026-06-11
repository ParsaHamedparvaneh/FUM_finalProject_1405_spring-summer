package packages;

import java.io.Serializable;
import java.util.*;

public class Market implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, Integer> prices;
    private Map<String, Integer> turnsSinceLastPurchase;
    private int currentTurn;
    
    public Market() {
        prices = new HashMap<>();
        turnsSinceLastPurchase = new HashMap<>();
        for (String resource : Constants.RESOURCES) {
            prices.put(resource, Constants.BASE_PRICE);
            turnsSinceLastPurchase.put(resource, 0);
        }
        currentTurn = 0;
    }
    
    public int getPrice(String resource) {
        return prices.getOrDefault(resource, Constants.BASE_PRICE);
    }
    
    public void newTurn() {
        currentTurn++;
        for (String resource : Constants.RESOURCES) {
            int turns = turnsSinceLastPurchase.get(resource);
            if (turns >= Constants.DECAY_THRESHOLD && getPrice(resource) > Constants.MIN_PRICE) {
                int newPrice = Math.max(Constants.MIN_PRICE, prices.get(resource) - Constants.PRICE_INCREASE);
                prices.put(resource, newPrice);
            }
            turnsSinceLastPurchase.put(resource, turns + 1);
        }
    }
    
    public boolean buyResource(Player player, String resource) {
        int price = getPrice(resource);
        
        // Check for Trader role discount
        int effectivePrice = price;
        if (player.getRole() != null && player.getRole().equals("Trader")) {
            effectivePrice = (int) Math.ceil(price * 3.0 / 4.0); // 3:4 ratio = 25% discount
        }
        
        if (player.getResources().getCount(Constants.RESOURCE_CAPITAL) >= effectivePrice) {
            player.getResources().removeResource(Constants.RESOURCE_CAPITAL, effectivePrice);
            player.getResources().addResource(resource, 1);
            
            // Update market price
            int newPrice = Math.min(Constants.MAX_PRICE, price + Constants.PRICE_INCREASE);
            prices.put(resource, newPrice);
            turnsSinceLastPurchase.put(resource, 0);
            
            if (effectivePrice != price) {
                System.out.println("Trader discount applied! Paid " + effectivePrice + " instead of " + price);
            }
            return true;
        }
        return false;
    }
    
    public void sellResource(Player player, String resource, int amount) {
        if (player.getResources().removeResource(resource, amount)) {
            int price = getPrice(resource);
            int capitalGain = price * amount;
            player.getResources().addResource(Constants.RESOURCE_CAPITAL, capitalGain);
            
            // Price decreases slightly when selling
            int newPrice = Math.max(Constants.MIN_PRICE, getPrice(resource) - 1);
            prices.put(resource, newPrice);
        }
    }
    
    public Map<String, Integer> getAllPrices() {
        return new HashMap<>(prices);
    }
    
    public Map<String, Integer> getTurnsSinceLastPurchase() {
        return new HashMap<>(turnsSinceLastPurchase);
    }
    
    public void restorePrices(Map<String, Integer> savedPrices) {
        if (savedPrices != null) {
            for (Map.Entry<String, Integer> entry : savedPrices.entrySet()) {
                prices.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public void restoreTurnsSinceLastPurchase(Map<String, Integer> savedTurns) {
        if (savedTurns != null) {
            for (Map.Entry<String, Integer> entry : savedTurns.entrySet()) {
                turnsSinceLastPurchase.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public int getCurrentTurn() {
        return currentTurn;
    }
    
    public void setCurrentTurn(int turn) {
        this.currentTurn = turn;
    }
}