/*-
 * #%L
 * HAPI FHIR JPA Server - Firely Query Language
 * %%
 * Copyright (C) 2014 - 2023 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.jpa.fql.parser;

import ca.uhn.fhir.model.api.IModelJson;
import ca.uhn.fhir.util.ValidateUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HfqlStatement implements IModelJson {

	@JsonProperty("select")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<SelectClause> mySelectClauses = new ArrayList<>();
	@JsonProperty("where")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<HavingClause> myWhereClauses = new ArrayList<>();
	@JsonProperty("having")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<HavingClause> myHavingClauses = new ArrayList<>();
	@JsonProperty("groupBy")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<String> myGroupByClauses = new ArrayList<>();
	@JsonProperty("orderBy")
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<OrderByClause> myOrderByClauses = new ArrayList<>();
	@JsonProperty("fromResourceName")
	private String myFromResourceName;
	@JsonProperty("limit")
	private Integer myLimit;

	public List<SelectClause> getSelectClauses() {
		return mySelectClauses;
	}

	public String getFromResourceName() {
		return myFromResourceName;
	}

	public void setFromResourceName(String theFromResourceName) {
		myFromResourceName = theFromResourceName;
	}

	@Nonnull
	public SelectClause addSelectClause(@Nonnull String theClause) {
		SelectClauseOperator operator = SelectClauseOperator.SELECT;
		return addSelectClause(theClause, operator);
	}

	@Nonnull
	public SelectClause addSelectClause(@Nonnull String theClause, @Nonnull SelectClauseOperator operator) {
		SelectClause clause = new SelectClause();
		clause.setClause(theClause);
		clause.setAlias(theClause);
		clause.setOperator(operator);
		mySelectClauses.add(clause);
		return clause;
	}

	public HavingClause addHavingClause() {
		HavingClause clause = new HavingClause();
		myHavingClauses.add(clause);
		return clause;
	}

	public List<HavingClause> getHavingClauses() {
		return myHavingClauses;
	}

	public HavingClause addWhereClause() {
		HavingClause clause = new HavingClause();
		myWhereClauses.add(clause);
		return clause;
	}

	public List<HavingClause> getWhereClauses() {
		return myWhereClauses;
	}

	@Nullable
	public Integer getLimit() {
		return myLimit;
	}

	public void setLimit(Integer theLimit) {
		myLimit = theLimit;
	}

	public HavingClause addHavingClause(String theLeft, WhereClauseOperatorEnum theOperator, String theRight) {
		HavingClause clause = addHavingClause(theLeft, theOperator);
		clause.addRight(theRight);
		return clause;
	}

	@Nonnull
	public HavingClause addHavingClause(String theLeft, WhereClauseOperatorEnum theOperator) {
		HavingClause clause = addHavingClause();
		clause.setLeft(theLeft);
		clause.setOperator(theOperator);
		return clause;
	}

	public void addGroupByClause(String theGroupByClause) {
		ValidateUtil.isNotBlankOrThrowIllegalArgument(theGroupByClause, "theGroupByClause must not be null or blank");
		getGroupByClauses().add(theGroupByClause);
	}

	public List<String> getGroupByClauses() {
		if (myGroupByClauses == null) {
			myGroupByClauses = new ArrayList<>();
		}
		return myGroupByClauses;
	}

	public boolean hasCountClauses() {
		return getSelectClauses().stream().anyMatch(t -> t.getOperator() == SelectClauseOperator.COUNT);
	}

	public OrderByClause addOrderByClause(String theClause, boolean theAscending) {
		ValidateUtil.isNotBlankOrThrowIllegalArgument(theClause, "theClause must not be null or blank");
		OrderByClause clause = new OrderByClause();
		clause.setClause(theClause);
		clause.setAscending(theAscending);
		getOrderByClauses().add(clause);
		return clause;
	}

	public List<OrderByClause> getOrderByClauses() {
		if (myOrderByClauses == null) {
			myGroupByClauses = new ArrayList<>();
		}
		return myOrderByClauses;
	}

	public int findSelectClauseIndex(String theClause) {
		for (int i = 0; i < getSelectClauses().size(); i++) {
			if (theClause.equals(getSelectClauses().get(i).getClause()) || theClause.equals(getSelectClauses().get(i).getAlias())) {
				return i;
			}
		}
		return -1;
	}

	public enum WhereClauseOperatorEnum {
		EQUALS,
		UNARY_BOOLEAN, IN
	}

	public enum SelectClauseOperator {

		SELECT,
		COUNT

	}

	public class OrderByClause implements IModelJson {

		@JsonProperty("clause")
		private String myClause;
		@JsonProperty("ascending")
		private boolean myAscending;

		public String getClause() {
			return myClause;
		}

		public void setClause(String theClause) {
			myClause = theClause;
		}

		public boolean isAscending() {
			return myAscending;
		}

		public void setAscending(boolean theAscending) {
			myAscending = theAscending;
		}

	}

	public static class SelectClause implements IModelJson {
		@JsonProperty("clause")
		private String myClause;
		@JsonProperty("alias")
		private String myAlias;
		@JsonProperty("operator")
		private SelectClauseOperator myOperator;

		/**
		 * Constructor
		 */
		public SelectClause() {
			// nothing
		}

		/**
		 * Constructor
		 *
		 * @param theClause The clause (will be used as both the clause and the alias)
		 */
		public SelectClause(String theClause) {
			setOperator(SelectClauseOperator.SELECT);
			setClause(theClause);
			setAlias(theClause);
		}

		public SelectClauseOperator getOperator() {
			return myOperator;
		}

		public void setOperator(SelectClauseOperator theOperator) {
			myOperator = theOperator;
		}

		public String getAlias() {
			return myAlias;
		}

		public void setAlias(String theAlias) {
			myAlias = theAlias;
		}

		public String getClause() {
			return myClause;
		}

		public void setClause(String theClause) {
			myClause = theClause;
		}
	}

	public static class HavingClause implements IModelJson {

		@JsonProperty("left")
		private String myLeft;
		@JsonProperty("operator")
		private WhereClauseOperatorEnum myOperator;
		@JsonProperty("right")
		private List<String> myRight = new ArrayList<>();

		public WhereClauseOperatorEnum getOperator() {
			return myOperator;
		}

		public void setOperator(WhereClauseOperatorEnum theOperator) {
			myOperator = theOperator;
		}

		public String getLeft() {
			return myLeft;
		}

		public void setLeft(String theLeft) {
			myLeft = theLeft;
		}

		public List<String> getRight() {
			return myRight;
		}

		public void addRight(String theRight) {
			myRight.add(theRight);
		}

		/**
		 * Returns the {@link #getRight() right} values as raw strings. That
		 * means that any surrounding quote marks are stripped.
		 */
		public Collection<String> getRightAsStrings() {
			ArrayList<String> retVal = new ArrayList<>();
			for (String next : getRight()) {
				if (next.startsWith("'")) {
					next = next.substring(1, next.length() - 1);
				}
				retVal.add(next);
			}
			return retVal;
		}

	}


}