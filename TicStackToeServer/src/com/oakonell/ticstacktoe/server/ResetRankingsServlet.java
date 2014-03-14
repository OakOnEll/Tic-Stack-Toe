package com.oakonell.ticstacktoe.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

public class ResetRankingsServlet extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		List<Entity> rankEntities = RanksQuery.getRankEntities(datastore);
		for (Entity each : rankEntities) {
			datastore.delete(each.getKey());
		}

		resp.setContentType("text/plain");
		resp.getWriter().println("{}");
	}

}
