package io.commerce.membershipService.storeCredit

import io.commerce.membershipService.core.ErrorCodeException
import io.commerce.membershipService.fixture.*
import io.commerce.membershipService.storeCredit.account.StoreCreditAccountRepository
import io.commerce.membershipService.storeCredit.transaction.TransactionNotes
import io.commerce.membershipService.storeCredit.transaction.TransactionRepository
import io.commerce.membershipService.storeCredit.transaction.TransactionType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@DataMongoTest
@EnableReactiveMongoAuditing
@StoreCreditRefundServiceTest
class StoreCreditRefundServiceRefundTest(
    private val storeCreditRefundService: StoreCreditRefundService,
    private val storeCreditAccountRepository: StoreCreditAccountRepository,
    private val transactionRepository: TransactionRepository
) : BehaviorSpec({

    val amount = 5_000
    val orderId = ObjectId.get()
    val chargeOrderId = ObjectId.get()
    val customerId = faker.random.nextUUID()
    val createdAt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusMonths(5).toInstant()

    val account = storeCreditAccount {
        this.customerId = customerId
        deposits = List(2) {
            storeCredit {
                this.amount = amount
            }
        }
    }
    val orderStoreCredit = orderStoreCredit {
        this.amount = amount
        this.transaction = orderTransactionView {
            this.customerId = customerId
            this.orderId = chargeOrderId
            this.type = TransactionType.CHARGE
            this.amount = amount
            this.createdAt = createdAt
        }
    }


    Given("????????? ????????????") {
        When("????????? ?????? ????????? ?????? ??????") {
            beforeEach {
                storeCreditAccountRepository.save(account)
            }

            afterEach {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
            }

            Then("enum class: MISSING_TRANSACTION") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit {
                        this.amount = 0
                        this.transaction = null
                    }
                }

                val result = shouldThrow<ErrorCodeException> {
                    storeCreditRefundService.refund(payload)
                }
                result.errorCode shouldBe StoreCreditError.MISSING_TRANSACTION.code
                result.reason shouldBe StoreCreditError.MISSING_TRANSACTION.message
            }

        }

        When("????????? ????????? ?????? ????????? ??????") {
            beforeEach {
                storeCreditAccountRepository.save(account)
            }

            afterEach {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
            }

            Then("enum class: INVALID_TRANSACTION") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit {
                        this.amount = 0
                        this.transaction = orderTransactionView {
                            this.customerId = customerId
                            this.orderId = chargeOrderId
                            this.type = TransactionType.CHARGE
                            this.amount = -1
                            this.createdAt = createdAt
                        }
                    }
                }

                val result = shouldThrow<ErrorCodeException> {
                    storeCreditRefundService.refund(payload)
                }
                result.errorCode shouldBe StoreCreditError.INVALID_TRANSACTION.code
                result.reason shouldBe StoreCreditError.INVALID_TRANSACTION.message
            }

        }

        When("????????? ????????? ????????? ??????") {
            beforeEach {
                storeCreditAccountRepository.save(account)
            }

            afterEach {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
            }

            Then("????????? ?????? balance: 15_000") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit
                }

                val result = storeCreditRefundService.refund(payload)
                result.balance shouldBe 15_000
            }

            Then("????????? ???????????? ???????????? ???????????? ????????????") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit
                }
                val result = storeCreditRefundService.refund(payload).deposits
                result.forExactly(1) { storeCredit ->
                    storeCredit.issuedAt shouldBe createdAt
                }
            }

            Then("????????? ???????????? ???????????? ??????????????? ?????? ID??? ????????????") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit
                }
                val result = storeCreditRefundService.refund(payload).deposits
                result.forExactly(1) { storeCredit ->
                    storeCredit.orderId shouldNotBe null
                }
            }

            Then("????????? ?????? ?????? 1?????? ????????????.") {
                val payload = orderRefundPayload {
                    this.orderId = orderId
                    this.storeCredit = orderStoreCredit
                }
                storeCreditRefundService.refund(payload)
                val transactions = transactionRepository.findAll().toList()
                transactions.forOne { transaction ->
                    transaction.id shouldNotBe null
                    transaction.orderId shouldNotBe null
                    transaction.customerId shouldBe customerId
                    transaction.amount shouldBe amount
                    transaction.type shouldBe TransactionType.DEPOSIT
                    transaction.note shouldBe TransactionNotes.STORE_CREDIT_REFUND
                    transaction.createdAt shouldNotBe null
                }
            }
        }
    }
})
