package io.commerce.membershipService.api

import io.commerce.membershipService.core.ErrorResponse
import io.commerce.membershipService.core.SecurityConstants
import io.commerce.membershipService.fixture.faker
import io.commerce.membershipService.fixture.order
import io.commerce.membershipService.fixture.refundPayload
import io.commerce.membershipService.fixture.storeCreditAccount
import io.commerce.membershipService.order.OrderError
import io.commerce.membershipService.order.OrderRepository
import io.commerce.membershipService.storeCredit.account.StoreCreditAccountError
import io.commerce.membershipService.storeCredit.account.StoreCreditAccountRepository
import io.commerce.membershipService.storeCredit.transaction.TransactionRepository
import io.commerce.membershipService.storeCredit.transaction.TransactionType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.toList
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
@Import(TestChannelBinderConfiguration::class)
class RefundStoreCreditIT(
    private val storeCreditAccountRepository: StoreCreditAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val orderRepository: OrderRepository,
    private val webTestClient: WebTestClient
) : BehaviorSpec({
    val orderNumber = "HF-11231312321"
    val tokenSubject = faker.random.nextUUID()
    val path = "/admin/membership/store-credit/account/$tokenSubject/refund"

    fun getOpaqueToken(authority: String = SecurityConstants.SERVICE_ADMIN) =
        SecurityMockServerConfigurers.mockOpaqueToken()
            .authorities(SimpleGrantedAuthority(authority))
            .attributes { it["sub"] = tokenSubject }

    Given("????????? ????????? ?????? API ?????? ??????") {
        When("????????? ?????? ??????") {
            val request = webTestClient
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)

            Then("401 Unauthorized") {
                request
                    .exchange()
                    .expectStatus().isUnauthorized
            }
        }

        When("????????? 'service-admin' ?????? ??????") {
            val request = webTestClient
                .mutateWith(getOpaqueToken(SecurityConstants.CUSTOMER))
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)

            Then("403 Forbidden") {
                request
                    .exchange()
                    .expectStatus().isForbidden
            }
        }
    }

    Given("????????? ????????? ?????? API ?????????") {
        When("?????? ????????? ?????? ??????") {
            val request = webTestClient
                .mutateWith(getOpaqueToken())
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    refundPayload {
                        this.orderNumber = ""
                        this.amount = 0
                        this.note = ""
                    }
                )

            afterTest {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
                orderRepository.deleteAll()
            }

            Then("status: 400 Bad Request") {
                request.exchange()
                    .expectStatus().isBadRequest
            }

            Then("body: ?????? ?????? ??????") {
                request.exchange()
                    .expectBody(ErrorResponse::class.java)
                    .returnResult()
                    .responseBody!!.fields.count() shouldBe 3
            }
        }

        When("???????????? ?????? ??????") {
            val request = webTestClient
                .mutateWith(getOpaqueToken())
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    refundPayload {
                        this.orderNumber = "123123123123"
                        this.amount = 5_000
                        this.note = "????????? ????????? ??????"
                    }
                )

            beforeTest {
                orderRepository.save(
                    order {
                        this.number = orderNumber
                        this.customerId = tokenSubject
                    }
                )
            }

            afterTest {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
                orderRepository.deleteAll()
            }

            Then("status: 400 Bad Request") {
                request.exchange()
                    .expectStatus().isBadRequest
            }

            Then("enum class: OrderError.ORDER_NOT_FOUND") {
                request.exchange()
                    .expectBody(ErrorResponse::class.java)
                    .returnResult()
                    .responseBody!!.should {
                    it.code shouldBe OrderError.ORDER_NOT_FOUND.code
                    it.message shouldBe OrderError.ORDER_NOT_FOUND.message
                }
            }
        }

        When("????????? ????????? ?????? ??????") {
            val request = webTestClient
                .mutateWith(getOpaqueToken())
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    refundPayload {
                        this.orderNumber = orderNumber
                        this.amount = 5_000
                        this.note = "????????? ????????? ??????"
                    }
                )

            beforeTest {
                orderRepository.save(
                    order {
                        this.number = orderNumber
                        this.customerId = tokenSubject
                    }
                )
            }

            afterTest {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
                orderRepository.deleteAll()
            }

            Then("status: 400 Bad Request") {
                request.exchange()
                    .expectStatus().isBadRequest
            }

            Then("enum class: StoreCreditAccountError.ACCOUNT_NOT_FOUND") {
                request.exchange()
                    .expectBody(ErrorResponse::class.java)
                    .returnResult()
                    .responseBody!!.should {
                    it.code shouldBe StoreCreditAccountError.ACCOUNT_NOT_FOUND.code
                    it.message shouldBe StoreCreditAccountError.ACCOUNT_NOT_FOUND.message
                }
            }
        }

        When("????????? ?????? ????????? ??????") {
            val amount = 5_000
            val note = "????????? ????????? ??????"
            val request = webTestClient
                .mutateWith(getOpaqueToken())
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    refundPayload {
                        this.orderNumber = orderNumber
                        this.amount = amount
                        this.note = note
                    }
                )

            beforeTest {
                orderRepository.save(
                    order {
                        this.number = orderNumber
                        this.customerId = tokenSubject
                    }
                )
                storeCreditAccountRepository.save(
                    storeCreditAccount {
                        this.customerId = tokenSubject
                    }
                )
            }

            afterTest {
                storeCreditAccountRepository.deleteAll()
                transactionRepository.deleteAll()
                orderRepository.deleteAll()
            }

            Then("status: 204 No Content") {
                request.exchange()
                    .expectStatus().isNoContent
                    .expectBody().isEmpty
            }

            Then("????????? ?????? balance 5_000 ??????") {
                request.exchange()
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

                storeCreditAccountRepository.findByCustomerId(tokenSubject)!!.should { storeCreditAccount ->
                    storeCreditAccount.customerId shouldBe tokenSubject
                    storeCreditAccount.balance shouldBe amount
                }
            }

            Then("????????? ?????? ?????? ????????? ????????? ?????? ID??? ????????????") {
                request.exchange()
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

                storeCreditAccountRepository.findByCustomerId(tokenSubject)!!.deposits
                    .forExactly(1) { storeCredit ->
                        storeCredit.orderId shouldNotBe null
                    }
            }

            Then("????????? ?????? ?????? ????????? ??????????????? amount, balance ??? 5000 ??????") {
                request.exchange()
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

                storeCreditAccountRepository.findByCustomerId(tokenSubject)!!.deposits
                    .forExactly(1) { storeCredit ->
                        storeCredit.amount shouldBe amount
                        storeCredit.balance shouldBe amount
                    }
            }

            Then("????????? ?????? ????????? ????????????") {
                request.exchange()
                    .expectStatus().isNoContent
                    .expectBody().isEmpty

                transactionRepository.findAll().toList().forExactly(1) { transaction ->
                    transaction.customerId shouldBe tokenSubject
                    transaction.amount shouldBe amount
                    transaction.type shouldBe TransactionType.DEPOSIT
                }
            }
        }
    }
})
