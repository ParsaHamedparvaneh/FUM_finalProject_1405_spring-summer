package packages;

public enum Role
{
    TRADER("Trader", "Trades at 3:1 rate instead of 4:1"),
    CTO("CTO", "Upgrades to Unicorn with 1 less Cloud resource"),
    VC_FUNDED("VC-Funded", "Starts with +2 Capital, holds 9 cards before tax");
    
    private String displayName;
    private String description;
    
    Role(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName()       { return displayName; }
    public String getDescription()       { return description; }
    public int getTradeRateNumerator()   { return this == TRADER ? 3 : 4; }
    public int getTradeRateDenominator() { return this == TRADER ? 1 : 1; }
    public int getUnicornCloudDiscount() { return this == CTO ? 1 : 0; }
    public int getStartingCapitalBonus() { return this == VC_FUNDED ? 2 : 0; }
    public int getMaxCardsBeforeTax()    { return this == VC_FUNDED ? 9 : 7; }
}