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
import uk.gov.hmrc.mobiletaskscheduler.models.{Item, ScheduleRequest}
import uk.gov.hmrc.mobiletaskscheduler.repositories.ItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Succeeded
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ItemService @Inject()(
    itemRepository: ItemRepository
)(implicit ec: ExecutionContext) {

    private val logger = Logger(getClass)

    def addItem(request: ScheduleRequest): Future[Unit] = {
        val item = Item(
            s"some title ${UUID.randomUUID.toString}",
            "some subtitle",
            isSomething = true
            )
        itemRepository.pushNew(item, availableAt = request.instant).map(_ => ())
    }

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
        /*
         * process the item
         * if processing fails, do something like below in the recoverWith lambda
         * should probably also delete the item when marked as complete itemRepository.completeAndDelete(workItem.id)
         * for the purpose of a POC, I'm going to just mark as complete
         */
        itemRepository.complete(workItem.id, Succeeded).map(_ => ())
                      .recoverWith {
                          case NonFatal(e) =>
                              logger.error(s"Failed to process item ${workItem}", e)
                              itemRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_ => ())
                      }
    }
}
