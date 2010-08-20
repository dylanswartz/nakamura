/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

@Service(value = SearchServiceFactory.class)
@Component(name = "org.sakaiproject.nakamura.api.search.SearchServiceFactory", immediate = true, metatype = true, description = "%searchservicefactory.description", label = "%searchservicefactory.name")
public class SearchServiceFactoryImpl implements SearchServiceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchServiceFactoryImpl.class);

  /**
   * Creates a merged Row Iterator from 2 iterators.
   * 
   * @param iteratorA
   * @param iteratorB
   * @return
   */
  public RowIterator getMergedRowIterator(RowIterator iteratorA, RowIterator iteratorB) {
    return new MergedRowIterator(iteratorA, iteratorB);
  }

  /**
   * Gets a Row Iterator filtered for protected paths.
   * 
   * @param rowIterator
   * @return
   */
  public RowIterator getPathFilteredRowIterator(RowIterator rowIterator) {
    return new SakaiSearchRowIterator(rowIterator);
  }
  
  /**
   * This method will return a SearchResultSet that contains a paged rowIterator and the
   * total hit count from Lucene.
   * 
   * @param request
   * @param query
   * @return
   * @throws SearchException
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    try {
      // Get the query result.
      QueryResult rs = query.execute();

      // Extract the total hits from lucene
      long hits = SearchUtil.getHits(rs);

      // Do the paging on the iterator.
      SakaiSearchRowIterator iterator = new SakaiSearchRowIterator(rs.getRows());
      long start = SearchUtil.getPaging(request, hits);
      iterator.skip(start);

      // Return the result set.
      SearchResultSet srs = new SearchResultSetImpl(iterator, hits);
      return srs;
    } catch (RepositoryException e) {
      LOGGER.error("Unable to perform query.", e);
      throw new SearchException(500, "Unable to perform query.");
    }

  }
  
  /**
   * Create a Search Result Set from a row iterator.
   * @param rowIterator
   * @param size
   * @return
   */
  public SearchResultSet getSearchResultSet(RowIterator rowIterator, long size) {
    return new SearchResultSetImpl(rowIterator, size);
  }


}
