/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaskscheduler.services

import play.api.Logger
import uk.gov.hmrc.mobiletaskscheduler.models.Item
import uk.gov.hmrc.mobiletaskscheduler.repositories.ItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Succeeded
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ItemService @Inject()(
    itemRepository: ItemRepository
)(implicit ec: ExecutionContext) {

    private val logger = Logger(getClass)

    def addItem(item: Item): Future[Boolean] =
    //    itemRepository.pushNew(item, availableAt = Instant.now().plus(2, ChronoUnit.DAYS)).map(_ => true)
        itemRepository.pushNew(item).map(_ => true)

    def scanAll(implicit ec: ExecutionContext): Future[Int] = {
        def processNext(acc: Int): Future[Int] =
            itemRepository.pullOutstanding.flatMap {
                case None => Future.successful(acc)
                case Some(item) => processItem(item).flatMap(_ => processNext(acc + 1))
            }

        processNext(0)
    }

    private def processItem(workItem: WorkItem[Item]): Future[Unit] = {
        logger.info("********************************")
        logger.info(s"Processing item ${workItem.item.title}")
        logger.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&")
//        itemRepository.completeAndDelete(workItem.id).map(_ => ())
        itemRepository.complete(workItem.id, Succeeded).map(_ => ())
        // process the item
        // if processing fails, do something like below
//                      .recoverWith {
//                          case NonFatal(e) =>
//                              logger.error(s"Failed to process item ${workItem}", e)
//                              itemRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_ => Unit)
//                      }
    }
}
