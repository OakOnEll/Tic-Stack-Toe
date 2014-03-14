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
import com.oakonell.ticstacktoe.server.rank.RankingRater;

public class AdjustAIRankServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		String aiKeyStr = req.getParameter("aiKey");
		String gameTypeKeyStr = req.getParameter("aiType");
		String opponentRankStr = req.getParameter("opponentRank");
		String wonStr = req.getParameter("won");

		int opponentRank = Integer.parseInt(opponentRankStr);
		boolean won = wonStr.equals("1");

		Entity entity = RanksQuery.getAI(datastore, aiKeyStr);
		long originalAiRank = (Long) entity.getProperty(gameTypeKeyStr);

		RankingRater rater = new EloRanker();
		long newRank = rater.calculateRank(originalAiRank, opponentRank, won);
		entity.setProperty(gameTypeKeyStr, newRank);

		long newOpponentRank = rater.calculateRank(opponentRank,
				originalAiRank, !won);

		datastore.put(entity);

		String ranksJSon = RanksQuery.queryAIRanks(datastore);

		resp.setContentType("text/plain");
		resp.getWriter().println(ranksJSon);
	}
}
