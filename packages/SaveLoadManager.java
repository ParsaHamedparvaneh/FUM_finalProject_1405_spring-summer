package packages;

import java.io.*;
import java.util.*;

public class SaveLoadManager
{
    
    public static void saveGame(GameLogic game, String filepath) throws IOException
    {
        GameState state = new GameState();
        
        List<GameState.PlayerSaveData> playerData = new ArrayList<GameState.PlayerSaveData>();
        for (Player player : game.getPlayers())
        {
            GameState.PlayerSaveData pd = new GameState.PlayerSaveData();
            pd.id = player.getId();
            pd.name = player.getName();
            pd.resources = player.getResources().getAllResources();
            pd.role = player.getRole();
            
            for (Company company : player.getCompanies())
            {
                GameState.CompanySaveData cd = new GameState.CompanySaveData();
                cd.type = company instanceof Unicorn ? "Unicorn" : "MVP";
                cd.ownerId = player.getId();
                cd.nodeX = company.getPosition().getPosition().x;
                cd.nodeY = company.getPosition().getPosition().y;
                pd.companies.add(cd);
            }
            
            for (Partnership partnership : player.getPartnerships())
            {
                GameState.PartnershipSaveData psd = new GameState.PartnershipSaveData();
                psd.ownerId = player.getId();
                psd.edgeX1 = partnership.getEdge().getNode1().getPosition().x;
                psd.edgeY1 = partnership.getEdge().getNode1().getPosition().y;
                psd.edgeX2 = partnership.getEdge().getNode2().getPosition().x;
                psd.edgeY2 = partnership.getEdge().getNode2().getPosition().y;
                pd.partnerships.add(psd);
            }
            
            playerData.add(pd);
        }
        state.setPlayers(playerData);
        
        state.setMarketPrices(game.getMarket().getAllPrices());
        state.setTurnsSinceLastPurchase(game.getMarket().getTurnsSinceLastPurchase());
        
        state.setCurrentPlayerIndex(game.getCurrentPlayerIndex());
        state.setCurrentTurn(game.getCurrentTurn());
        state.setEventLog(game.getEventLog());
        
        if (game.getAuditorPosition() != null)
            state.setAuditorPosition(new Point(game.getAuditorPosition().x, game.getAuditorPosition().y));
        
        Sector[][] map = game.getMap();
        GameState.SectorSaveData[][] sectorData = new GameState.SectorSaveData[Constants.MAP_HEIGHT][Constants.MAP_WIDTH];
        for (int i = 0; i < Constants.MAP_HEIGHT; i++)
        {
            for (int j = 0; j < Constants.MAP_WIDTH; j++)
            {
                Sector s = map[i][j];
                GameState.SectorSaveData sd = new GameState.SectorSaveData();
                sd.type = s.getType();
                sd.diceNumber = s.getDiceNumber();
                sd.hasAuditor = s.hasAuditor();
                sd.x = s.getX();
                sd.y = s.getY();
                sectorData[i][j] = sd;
            }
        }
        state.setSectors(sectorData);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath))) { oos.writeObject(state); }
    }
    
    public static GameLogic loadGame(String filepath) throws IOException, ClassNotFoundException
    {
        GameState state;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) { state = (GameState) ois.readObject(); }
        
        List<String> playerNames = new ArrayList<String>();
        for (GameState.PlayerSaveData pd : state.getPlayers())
            playerNames.add(pd.name);
        
        GameLogic game = new GameLogic(playerNames);
        
        for (GameState.PlayerSaveData pd : state.getPlayers())
        {
            Player player = game.getPlayerById(pd.id);
            if (player != null)
            {
                for (Map.Entry<String, Integer> entry : pd.resources.entrySet())
                    player.getResources().addResource(entry.getKey(), entry.getValue());

                player.setRole(pd.role);
            }
        }
        
        game.getMarket().restorePrices(state.getMarketPrices());
        game.getMarket().restoreTurnsSinceLastPurchase(state.getTurnsSinceLastPurchase());
        
        game.setCurrentPlayerIndex(state.getCurrentPlayerIndex());
        game.setCurrentTurn(state.getCurrentTurn());
        game.setEventLog(state.getEventLog());
        
        if (state.getAuditorPosition() != null)
        {
            game.setAuditorPosition(state.getAuditorPosition());
            Sector sector = game.getMap()[state.getAuditorPosition().x][state.getAuditorPosition().y];
            if (sector != null)
                sector.setAuditor(true);
        }
        
        return game;
    }
}