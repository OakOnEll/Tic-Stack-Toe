package com.oakonell.ticstacktoe.server;

import java.util.Iterator;
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
	private static final String AI_ID_RANDOM = "-1";
	private static final String AI_ID_EASY = "1";
	private static final String AI_ID_MEDIUM = "2";
	private static final String AI_ID_HARD = "3";

	public static final String AI_RANK_KIND = "Ranks";
	public static final String AI_RANK_NAME = "AI";
	public static final String AI_RANK_ENTITY = "Rank";

	public static final String AI_RANK_ENTITY_PROPERTY_GAME_TYPE = "aiType";
	public static final String AI_RANK_ENTITY_PROPERTY_KEY = "aiKey";

	public static final String AI_RANK_ENTITY_PROPERTY_JUNIOR_RANK = "aiJuniorRank";
	public static final String AI_RANK_ENTITY_PROPERTY_NORMAL_RANK = "aiNormalRank";
	public static final String AI_RANK_ENTITY_PROPERTY_STRICT_RANK = "aiStrictRank";

	public static String queryAIRanks(DatastoreService datastore) {

		List<Entity> ranks = getRankEntities(datastore);
		StringBuilder builder = new StringBuilder("{");
		if (!ranks.isEmpty()) {
			for (Iterator<Entity> iter = ranks.iterator(); iter.hasNext();) {
				Entity rank = iter.next();
				String aiKeyProperty = (String) rank
						.getProperty(AI_RANK_ENTITY_PROPERTY_KEY);
				long aiJuniorProperty = (Long) rank
						.getProperty(AI_RANK_ENTITY_PROPERTY_JUNIOR_RANK);
				long aiNormalProperty = (Long) rank
						.getProperty(AI_RANK_ENTITY_PROPERTY_NORMAL_RANK);
				long aiStrictProperty = (Long) rank
						.getProperty(AI_RANK_ENTITY_PROPERTY_STRICT_RANK);
				appendAiRank(builder, aiKeyProperty, (short) aiJuniorProperty,
						(short) aiNormalProperty, (short) aiStrictProperty);
				if (iter.hasNext()) {
					builder.append(", ");
				}
			}
		} else {
			Key aiKey = KeyFactory.createKey(AI_RANK_KIND, AI_RANK_NAME);

			// insert rows
			insertAndAppendAiRank(aiKey, builder, datastore, AI_ID_RANDOM,
					(short) 620, (short) 620, (short) 650);
			builder.append(", ");
			insertAndAppendAiRank(aiKey, builder, datastore, AI_ID_EASY,
					(short) 1560, (short) 1388, (short) 1450);
			builder.append(", ");
			insertAndAppendAiRank(aiKey, builder, datastore, AI_ID_MEDIUM,
					(short) 1850, (short) 1850, (short) 1800);
			builder.append(", ");
			insertAndAppendAiRank(aiKey, builder, datastore, AI_ID_HARD,
					(short) 2050, (short) 2200, (short) 2200);
		}
		builder.append("}");
		String ranksJSon = builder.toString();
		return ranksJSon;
	}

	public static List<Entity> getRankEntities(DatastoreService datastore) {
		Key aiKey = KeyFactory.createKey(AI_RANK_KIND, AI_RANK_NAME);

		Query query = new Query(AI_RANK_ENTITY, aiKey).addSort(
				AI_RANK_ENTITY_PROPERTY_KEY, Query.SortDirection.ASCENDING);
		List<Entity> ranks = datastore.prepare(query).asList(
				FetchOptions.Builder.withLimit(10));
		return ranks;
	}

	private static void insertAndAppendAiRank(Key aiKey, StringBuilder builder,
			DatastoreService datastore, String aiKeyProperty,
			short aiJuniorRankProperty, short aiNormalRankProperty,
			short aiStrictRankProperty) {
		Entity entity = new Entity(AI_RANK_ENTITY, aiKey);
		entity.setProperty(AI_RANK_ENTITY_PROPERTY_KEY, aiKeyProperty);
		entity.setProperty(AI_RANK_ENTITY_PROPERTY_JUNIOR_RANK,
				aiJuniorRankProperty);
		entity.setProperty(AI_RANK_ENTITY_PROPERTY_NORMAL_RANK,
				aiNormalRankProperty);
		entity.setProperty(AI_RANK_ENTITY_PROPERTY_STRICT_RANK,
				aiStrictRankProperty);
		datastore.put(entity);
		appendAiRank(builder, aiKeyProperty, aiJuniorRankProperty,
				aiNormalRankProperty, aiStrictRankProperty);
	}

	private static void appendAiRank(StringBuilder builder,
			String aiKeyProperty, short aiJuniorRankProperty,
			short aiNormalRankProperty, short aiStrictRankProperty) {
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
		builder.append("] ");
	}

	public static Entity getAI(DatastoreService datastore, String aiKeyStr) {
		Key aiKey = KeyFactory.createKey(AI_RANK_KIND, AI_RANK_NAME);

		Query query = new Query(AI_RANK_ENTITY, aiKey);

		Filter aiKeyFilter = new FilterPredicate(AI_RANK_ENTITY_PROPERTY_KEY,
				FilterOperator.EQUAL, aiKeyStr);

		query.setFilter(aiKeyFilter);
		List<Entity> ranks = datastore.prepare(query).asList(
				FetchOptions.Builder.withLimit(2));

		if (ranks.isEmpty()) {
			throw new RuntimeException("There is no rank for id '" + aiKeyStr
					+ "'!");
		}
		if (ranks.size() > 1) {
			throw new RuntimeException(
					"there are multiple rank entities for id '" + aiKeyStr
							+ "'! Found " + ranks.size());
		}
		return ranks.get(0);
	}
}
