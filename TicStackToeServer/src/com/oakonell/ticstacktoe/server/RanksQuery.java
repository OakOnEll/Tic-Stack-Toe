package com.oakonell.ticstacktoe.server;

import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class RanksQuery {

	public static String queryAIRanks(DatastoreService datastore) {

		List<Entity> ranks = getRankEntities(datastore);
		Key aiKey = KeyFactory.createKey("Ranks", "AI");
		StringBuilder builder = new StringBuilder("{");
		if (!ranks.isEmpty()) {
			for (Entity rank : ranks) {
				String aiKeyProperty = (String) rank.getProperty("aiKey");
				long aiJuniorProperty = (Long) rank.getProperty("aiJuniorRank");
				long aiNormalProperty = (Long) rank.getProperty("aiNormalRank");
				long aiStrictProperty = (Long) rank.getProperty("aiStrictRank");
				appendAiRank(builder, aiKeyProperty, aiJuniorProperty,
						aiNormalProperty, aiStrictProperty);
			}
		} else {
			// insert rows
			insertAndAppendAiRank(aiKey, builder, datastore, "-1", 620, 620,
					650);
			insertAndAppendAiRank(aiKey, builder, datastore, "1", 1560, 1388,
					1450);
			insertAndAppendAiRank(aiKey, builder, datastore, "2", 1850, 1850,
					1800);
			insertAndAppendAiRank(aiKey, builder, datastore, "3", 2050, 2200,
					2200);
		}
		builder.append("}");
		String ranksJSon = builder.toString();
		return ranksJSon;
	}

	public static List<Entity> getRankEntities(DatastoreService datastore) {
		Key aiKey = KeyFactory.createKey("Ranks", "AI");

		Query query = new Query("Rank", aiKey).addSort("aiKey",
				Query.SortDirection.ASCENDING);
		List<Entity> ranks = datastore.prepare(query).asList(
				FetchOptions.Builder.withLimit(10));
		return ranks;
	}

	private static void insertAndAppendAiRank(Key aiKey, StringBuilder builder,
			DatastoreService datastore, String aiKeyProperty,
			long aiJuniorRankProperty, long aiNormalRankProperty,
			long aiStrictRankProperty) {
		Entity entity = new Entity("Rank", aiKey);
		entity.setProperty("aiKey", aiKeyProperty);
		entity.setProperty("aiJuniorRank", aiJuniorRankProperty);
		entity.setProperty("aiNormalRank", aiNormalRankProperty);
		entity.setProperty("aiStrictRank", aiStrictRankProperty);
		datastore.put(entity);
		appendAiRank(builder, aiKeyProperty, aiJuniorRankProperty,
				aiNormalRankProperty, aiStrictRankProperty);
	}

	private static void appendAiRank(StringBuilder builder,
			String aiKeyProperty, long aiJuniorRankProperty,
			long aiNormalRankProperty, long aiStrictRankProperty) {
		builder.append("'");
		builder.append(aiKeyProperty);
		builder.append("':[");
		builder.append(" ");
		builder.append(aiJuniorRankProperty);
		builder.append(", ");
		builder.append(aiNormalRankProperty);
		builder.append(", ");
		builder.append(aiStrictRankProperty);
		builder.append(", ");
		builder.append("], ");
	}

	public static Entity getAI(DatastoreService datastore, String aiKeyStr) {
		Key aiKey = KeyFactory.createKey("Ranks", "AI");

		Query query = new Query("Rank", aiKey);

		Filter aiKeyFilter = new FilterPredicate("aiKey", FilterOperator.EQUAL,
				aiKeyStr);

		query.setFilter(aiKeyFilter);
		List<Entity> ranks = datastore.prepare(query).asList(
				FetchOptions.Builder.withLimit(2));

		if (ranks.isEmpty()) {
			throw new RuntimeException("Should have a rank for " + aiKeyStr
					+ "!");
		}
		if (ranks.size() > 1) {
			throw new RuntimeException("Should have a single rank for "
					+ aiKeyStr + "! But found " + ranks.size());
		}
		return ranks.get(0);
	}
}
