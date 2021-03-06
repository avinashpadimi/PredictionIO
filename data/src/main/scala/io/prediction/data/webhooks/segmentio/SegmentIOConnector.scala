/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.data.webhooks.segmentio

import io.prediction.data.webhooks.JsonConnector
import io.prediction.data.webhooks.ConnectorException

import org.json4s.Formats
import org.json4s.DefaultFormats
import org.json4s.JObject

private[prediction] object SegmentIOConnector extends JsonConnector {

  implicit val json4sFormats: Formats = DefaultFormats

  override
  def toEventJson(data: JObject): JObject = {
    // TODO: check segmentio API version
    val common = try {
      data.extract[Common]
    } catch {
      case e: Exception => throw new ConnectorException(
          s"Cannot extract Common field from ${data}. ${e.getMessage()}", e)
    }

    try {
      common.`type` match {
        case "identify" => toEventJson(
          common = common,
          identify = data.extract[Identify])

        // TODO: support other events
        case _ => throw new ConnectorException(
          s"Cannot convert unknown type ${common.`type`} to event JSON.")
      }
    } catch {
      case e: ConnectorException => throw e
      case e: Exception => throw new ConnectorException(
        s"Cannot convert ${data} to event JSON. ${e.getMessage()}", e)
    }
  }

  def toEventJson(common: Common, identify: Identify): JObject = {

    import org.json4s.JsonDSL._

    val json =
      ("event" -> common.`type`) ~
      ("entityType" -> "user") ~
      ("entityId" -> identify.userId) ~
      ("eventTime" -> common.timestamp) ~
      ("properties" -> (
        ("context" -> common.context) ~
        ("traits" -> identify.traits)
      ))
    json
  }

}

private[prediction] case class Common(
  `type`: String,
  context: Option[JObject],
  timestamp: String
  // TODO: add more fields
)

private[prediction] case class Identify(
  userId: String,
  traits: Option[JObject]
  // TODO: add more fields
)
