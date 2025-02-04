package org.babyfish.jimmer.example.kt.graphql.entities

import org.babyfish.jimmer.sql.*
import java.math.BigDecimal
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive

@Entity
interface Book : CommonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    @Key
    val name: @NotBlank String

    @Key
    val edition: @Positive Int

    val price: BigDecimal

    @ManyToOne
    val store: BookStore?

    @ManyToMany
    @JoinTable(
        name = "BOOK_AUTHOR_MAPPING",
        joinColumnName = "BOOK_ID",
        inverseJoinColumnName = "AUTHOR_ID"
    )
    val authors: List<Author>
}