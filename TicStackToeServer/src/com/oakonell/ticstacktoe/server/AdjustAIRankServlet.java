package com.oakonell.ticstacktoe.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.oakonell.ticstacktoe.server.rank.EloRanker;
import com.oakonell.ticstacktoe.server.rank.GameOutcome;
import com.oakonell.ticstacktoe.server.rank.RankingRater;

public class AdjustAIRankServlet extends HttpServlet {
	private static final String PARAM_OUTCOME = "outcome";
	private static final String PARAM_OPPONENT_RANK = "opponentRank";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String aiKeyStr = req
				.getParameter(RanksQuery.AI_RANK_ENTITY_PROPERTY_KEY);
		String gameTypeKeyStr = req
				.getParameter(RanksQuery.AI_RANK_ENTITY_PROPERTY_GAME_TYPE);
		String opponentRankStr = req.getParameter(PARAM_OPPONENT_RANK);
		String outcomeStr = req.getParameter(PARAM_OUTCOME);

		int outcomeInt = Integer.parseInt(outcomeStr);
		GameOutcome outcome = GameOutcome.fromId(outcomeInt);

		String typeProperty; 
		if (gameTypeKeyStr.equals("1")) {
			typeProperty = RanksQuery.AI_RANK_ENTITY_PROPERTY_JUNIOR_RANK;
		} else if (gameTypeKeyStr.equals("2")) {
			typeProperty = RanksQuery.AI_RANK_ENTITY_PROPERTY_NORMAL_RANK;
		} else if (gameTypeKeyStr.equals("3")) {
			typeProperty = RanksQuery.AI_RANK_ENTITY_PROPERTY_STRICT_RANK;
		} else {
			throw new RuntimeException("Invalid " + RanksQuery.AI_RANK_ENTITY_PROPERTY_GAME_TYPE + " parameter value " + gameTypeKeyStr);
		}

		short opponentRank = Short.parseShort(opponentRankStr);

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		Entity entity = RanksQuery.getAI(datastore, aiKeyStr);
		
		long originalAiRank = (Long) entity.getProperty(typeProperty);

		RankingRater rater = new EloRanker();
		short newRank = rater.calculateRank((short)originalAiRank, opponentRank,
				outcome);
		entity.setProperty(typeProperty, newRank);

		datastore.put(entity);

		String ranksJSon = RanksQuery.queryAIRanks(datastore);

		resp.setContentType("text/plain");
		resp.getWriter().println(ranksJSon);
	}
}
