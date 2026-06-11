package packages;

import java.io.Serializable;
import java.util.*;

public class Market implements Serializable {
    private static final long serialVersionUID = 2L;
    private Map<String,Integer> prices;
    private Map<String,Integer> turnsSinceLastPurchase;
    
    public Market() {
        prices = new HashMap<>();
        turnsSinceLastPurchase = new HashMap<>();
        for (String r : Constants.RESOURCES) {
            prices.put(r, Constants.BASE_PRICE);
            turnsSinceLastPurchase.put(r, 0);
        }
    }
    
    public int getPrice(String r) { return prices.getOrDefault(r, Constants.BASE_PRICE); }
    
    public void newTurn() {
        for (String r : Constants.RESOURCES) {
            int t = turnsSinceLastPurchase.get(r);
            if (t >= Constants.DECAY_THRESHOLD) {
                int newPrice = Math.max(Constants.MIN_PRICE, prices.get(r) - Constants.PRICE_INCREASE);
                prices.put(r, newPrice);
            }
            turnsSinceLastPurchase.put(r, t+1);
        }
    }
    
    public boolean buyResource(Player player, String resource) {
        int price = getPrice(resource);
        // Trader discount
        if (player.getRole() != null && player.getRole().equals("Trader")) {
            price = (int) Math.ceil(price * 3.0 / 4.0);
        }
        if (player.getResources().getCount(Constants.RESOURCE_CAPITAL) >= price) {
            player.getResources().removeResource(Constants.RESOURCE_CAPITAL, price);
            player.getResources().addResource(resource, 1);
            int newPrice = Math.min(Constants.MAX_PRICE, prices.get(resource) + Constants.PRICE_INCREASE);
            prices.put(resource, newPrice);
            turnsSinceLastPurchase.put(resource, 0);
            return true;
        }
        return false;
    }
    
    public void sellResource(Player player, String resource, int amount) {
        if (player.getResources().removeResource(resource, amount)) {
            int price = getPrice(resource);
            player.getResources().addResource(Constants.RESOURCE_CAPITAL, price * amount);
            int newPrice = Math.max(Constants.MIN_PRICE, price - 1);
            prices.put(resource, newPrice);
        }
    }
    
    public Map<String,Integer> getAllPrices() { return new HashMap<>(prices); }
    public Map<String,Integer> getTurnsSinceLastPurchase() { return new HashMap<>(turnsSinceLastPurchase); }
    public void restorePrices(Map<String,Integer> p) { prices.putAll(p); }
    public void restoreTurnsSinceLastPurchase(Map<String,Integer> t) { turnsSinceLastPurchase.putAll(t); }
}