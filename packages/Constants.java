package packages;

public class Constants
{
    public static final int MAP_HEIGHT  = 5;
    public static final int MAP_WIDTH   = 5;
    public static final int SECTOR_SIZE = 100;
    public static final int WIN_SCORE   = 10;
    
    public static final String SECTOR_TYPE_AI         = "AI Hub";
    public static final String SECTOR_TYPE_FINTECH    = "Fintech";
    public static final String SECTOR_TYPE_CLOUD      = "Cloud";
    public static final String SECTOR_TYPE_DATA       = "Data Valley";
    public static final String SECTOR_TYPE_PATENT     = "IP Quarter";
    public static final String SECTOR_TYPE_REGULATORY = "Regulatory Zone";
    
    public static final String[] SECTOR_TYPES = {SECTOR_TYPE_AI, SECTOR_TYPE_FINTECH, 
                                                 SECTOR_TYPE_CLOUD, SECTOR_TYPE_DATA ,
                                                 SECTOR_TYPE_PATENT                  };
    
    
    public static final int SCORE_MVP           = 1;
    public static final int SCORE_UNICORN       = 2;
    
    public static final String RESOURCE_TALENT  = "Talent";
    public static final String RESOURCE_CAPITAL = "Capital";
    public static final String RESOURCE_CLOUD   = "Cloud";
    public static final String RESOURCE_PATENT  = "Patent";
    public static final String RESOURCE_DATA    = "Data";
    
    public static final String[] RESOURCES = {RESOURCE_TALENT, RESOURCE_CAPITAL, 
                                              RESOURCE_CLOUD, RESOURCE_PATENT  ,
                                              RESOURCE_DATA                    };
    
    public static final String SECTOR_TO_RESOURCE[][] = {{SECTOR_TYPE_AI, RESOURCE_TALENT       },
                                                         {SECTOR_TYPE_FINTECH, RESOURCE_CAPITAL },
                                                         {SECTOR_TYPE_CLOUD, RESOURCE_CLOUD     },
                                                         {SECTOR_TYPE_DATA, RESOURCE_DATA       },
                                                         {SECTOR_TYPE_PATENT, RESOURCE_PATENT   }};
    
    public static final int BASE_PRICE      = 4;
    public static final int MAX_PRICE       = 6;
    public static final int MIN_PRICE       = 2;
    public static final int PRICE_INCREASE  = 1;
    public static final int DECAY_THRESHOLD = 3;
    
    public static final int MVP_COST_TALENT  = 1;
    public static final int MVP_COST_CAPITAL = 1;
    public static final int MVP_COST_CLOUD   = 1;
    public static final int MVP_COST_DATA    = 1;
    
    public static final int UNICORN_UPGRADE_CLOUD = 2;
    public static final int UNICORN_UPGRADE_DATA  = 3;
    
    public static final int PARTNERSHIP_COST_PATENT  = 1;
    public static final int PARTNERSHIP_COST_CAPITAL = 1;
    
    public static final int TRADER_TRADE_RATE_NUM = 3;
    public static final int TRADER_TRADE_RATE_DEN = 1;
    public static final int CLOUD_DISCOUNT        = 1;
    public static final int VC_EXTRA_CAPITAL      = 2;
    public static final int VC_MAX_CARDS          = 9;
    public static final int NORMAL_MAX_CARDS      = 7;
    
    public static final int ROLE_PENALTY     = -1;
    public static final int TAX_HALF_DIVISOR = 2;
    
    public static final String COLOR_TEXT = "#eeeeee";
    public static final String COLOR_BG   = "#000000";
}