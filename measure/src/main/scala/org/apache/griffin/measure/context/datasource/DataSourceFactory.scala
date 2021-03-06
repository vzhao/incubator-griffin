/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.griffin.measure.context.datasource

import org.apache.griffin.measure.Loggable
import org.apache.griffin.measure.configuration.params.DataSourceParam
import org.apache.griffin.measure.context.datasource.cache.DataSourceCacheFactory
import org.apache.griffin.measure.context.datasource.connector.{DataConnector, DataConnectorFactory}
import org.apache.griffin.measure.context.datasource.info.TmstCache
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext

import scala.util.Success

object DataSourceFactory extends Loggable {

  def getDataSources(sparkSession: SparkSession,
                     ssc: StreamingContext,
                     dataSourceParams: Seq[DataSourceParam]
                    ): Seq[DataSource] = {
    dataSourceParams.zipWithIndex.flatMap { pair =>
      val (param, index) = pair
      getDataSource(sparkSession, ssc, param, index)
    }
  }

  private def getDataSource(sparkSession: SparkSession,
                            ssc: StreamingContext,
                            dataSourceParam: DataSourceParam,
                            index: Int
                           ): Option[DataSource] = {
    val name = dataSourceParam.name
    val connectorParams = dataSourceParam.getConnectors
    val tmstCache = TmstCache()

    val dataSourceCacheOpt = DataSourceCacheFactory.getDataSourceCacheOpt(
      sparkSession.sqlContext, dataSourceParam.cache, name, index, tmstCache)

    val dataConnectors: Seq[DataConnector] = connectorParams.flatMap { connectorParam =>
      DataConnectorFactory.getDataConnector(sparkSession, ssc, connectorParam,
        tmstCache, dataSourceCacheOpt) match {
          case Success(connector) => Some(connector)
          case _ => None
        }
    }

    Some(DataSource(name, dataSourceParam, dataConnectors, dataSourceCacheOpt))
  }

}
