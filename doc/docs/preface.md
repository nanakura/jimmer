---
sidebar_position: 1
title: Preface
---

## 1. The premise of the discussion

A large part of the operations of OLTP type projects are aimed at the original data of the database. At this time, the object structure in the application is roughly the same as the data structure in the database, which is the scene discussed in this article.

The data types related to the calculation indicators introduced by business computing are not the same as the original structure of the database, and are not the scope of this article.

## 2. Disadvantages of the current factions

Now, there are many frameworks for users to access RDBMS, which are generally divided into two factions

- Traditional ORM faction, JPA is the most well-known representative.
- DTO Mapper faction, MyBatis is the most well-known representative.

### 2.1. Traditional ORM faction represented by JPA

In traditional ORM, developers create entity classes that directly correspond to database table structures. From a mapping standpoint, it's pretty straightforward.

Traditional ORM focuses on maintaining the relationship between objects, taking JPA as an example
```java
List<Book> books = entityManager
    .createQuery(
        "select book from Book book " +
        "left join fetch book.store " +
        "left join fetch book.authors",
        Book.class
    ).getResultList();
```
The `join fetch` in this example is a feature of JPA, which can use `SQL JOIN` to make the returned `Book` object no longer a single object, but with associated properties `store` and `authors`.

Through the optional `join fetch`* (or other tricks, different ORM frameworks support different ways)*, the traditional ORM can return either a single data object or a complex object with associations, which is actually clipping capability of data structures.

This clipping capability is based on object granularity, but each object in the returned data structure is complete, that is to say, it lacks the clipping capability at the simple property level.

Simple property level clipping capability cannot be achieved. When there are many object properties so that the efficiency of querying all columns is very low, or when important properties need to be desensitized to low-privileged users, it will become a problem. Unfortunately, real projects are like this.

:::note
Although Hibernate starts from 3.x, simple (non-associative) properties can also be set to lazy. However, this feature is designed for lob properties, not for simple property level clipping, and the flexibility is very limited. not discussed
:::

If you want the traditional ORM to accurately implement property-level clipping, you will use code like this

```java
List<BookDTO> bookDTOs = entityManager
    .createQuery(
	    "select new BookDTO(book.id, book.name) " +
        "from Book book",
        BookDTO.class
    ).getResultList();
```

In this example, we only want to query the `id` and `name` properties, we have to build a new type `BookDTO` which is  used as a carrier for the mutilated object with only two properties. At the same time when we get the simple property-level clipping ability, because `BookDTO` is a flat object rather than an entity object, the object-level clipping ability is lost.

:::note
It is precisely because this usage loses the core capabilities of ORM that it is a non-mainstream usage in traditional ORM practice and is rarely used.
:::

Another problem with traditional ORMs is that the returned data is very complex and difficult to use directly.

For unloaded lazy properties, developers can easily ignore them in Json serialization, that's not a problem.

The real trouble is that there are two-way associations between objects, whereas front-end and microservice clients prefer to see a tree of objects with only one-way associations.

For example, `TreeNode` entity has both an upward `parent` property and a downward `childNodes` property.

- Some businesses may need to query a node and all its subordinates, and return a data structure such as `aggregateRoot->childNodes->childNodes->...`;
- Some businesses query a node and all its superiors, and return a data structure like `aggregateRoot->parent->parent->...`.

Therefore, you cannot simply specify which of `parent` and `childNodes` is exposed and which is hidden. You can't solve this problem simply with the `@JsonIgnore` annotation, it's a very tricky one.

### 2.2. DTO Mapper faction represented by MyBatis

From the above description, we know that traditional ORM has two disadvantages.

1. The mainstream usage that facilitates the use of traditional ORM capabilities, although it has flexible object-level clipping capabilities, but it loses the simple property-level clipping capabilities.
2. The entity objects returned by the ORM are too complex to be returned directly and cannot interact with HTTP.

These two problems are caused by the weak expressive ability of data objects. In fact, they can be solved by defining the DTO classes required by a specific business senarios.

Why write code to convert ORM entities to DTOs when people are destined to define specific business-related DTO types? Why not just implement the mapping from SQL results to DTOs directly?

Therefore, the DTO Mapper faction is recognized by the developers, and this faction proposes a very different solution. Developers no longer define entity classes directly corresponding to the database structure, but directly define DTO types for each specific business senario, such as:

- Create class `Book` to express the lonely book objects
- Create class `BookWithStore` to express the book objects with the associated property `store`
- Create class `BookWithAuthors` to express the book objects with the associated property `authors`
- Create class `BookWithStoreAndAuthors` to represent book objects with associated properties `store` and `authors`

Each business API returns the DTO object it needs, and each API uses a specific SqlResultMapper to map specific query results to specific DTO.

However, this approach is equally problematic

1. In the above example, we only show object-level clipping, not simple property-level clipping, and the depth of the object tree is also very shallow. If not, the number of DTO types will be large, and it can even be described as **exploding**. At this time, there will be so many DTO classes that it is difficult to even get a name. Developers even need to incorporate industry-specific naming conventions to avoid very long class names.

2. There are too many DTOs. Although different DTOs are different, they have many similar parts, which are highly redundant. The system loses compactness, and development and testing costs balloon.

3. Once new requirements are introduced, the structure of the database changes, and multiple redundant services need to be modified.

To avoid problems 2 and 3, SQL mapper fragments or business code can be reused as much as possible, but this destroys the simplicity of the system and the code becomes difficult to understand, which is the inevitable price of excessive use of low-value reuse.

## 3. Advantages of Jimmer

From the above discussion, we know that

- Traditional ORM faction: The advantage is that it directly corresponds to the database structure and provides a unified perspective; but the disadvantage is that only object-level clipping is performed on the returned data format, and there is no simple property-level clipping, and the returned data structure is difficult to use directly.
- DTO Mapper faction: The advantage is that the DTO object is simple, and the data structure represented by the returned aggregate root only contains one-way associations; but the disadvantage is that the number of DTO types is seriously inflated, although different but similar, the development cost and testing cost are both very high.

Jimmer perfectly integrates the strengths of the two factions, and walks out of the another way. Therefore, Jimmer cannot be simply compared to any of solutation in the above factions.

### 3.1. No DTO Mode: Dynamic Entities

In jimmer

-   Entity objects are dynamic, any object property, whether simple or associated, can be missing.
    :::info
    For Jimmer's entity objects, not specifying a property and specifying a property as null are two completely different things.
    :::

-   Directly reading missing properties of an object in Java or Kotlin code will result in an exception; however, when JSON serialization, missing properties are automatically ignored without exception.

-   Although two-way associations can be defined between different types when declaring entity types; however, when a specific business senario needs to instantiate objects, only one-way associations can be created between entity objects, ensuring that any data structure can use a simple aggregate root object to express.

:::tip
Dynamic entity itself is not DTO, but it has all the characteristics of DTO object, any entity object tree can directly participate in HTTP interaction.

Dynamic entities are the architectural foundation of the entire ORM.
:::

### 3.2. Query arbitrarily complex data structures

It perfectly supports object-level and simple property-level clipping capabilities. Users can delineate a local data structure from the complete relational model of database, that is, an arbitrarily complex tree structure, and query the entire data structure by returning a dynamic entity tree.

:::tip
Let RDBMS have GraphQL-like functionality. Even if your project has nothing to do with GraphQL technology, your RDMBS has all its advantages.

Jimmer does a better job than GraphQL, it even supports recursive queries on self-associative properties.
:::

### 3.3. Modify arbitrarily complex data structures

Users can pass arbitrarily complex trees of dynamic objects to Jimmer, saving the entire tree in one sentence.

:::tip
It can be understood as the inverse function of GraphQL.
:::

### 3.3. Powerful caching mechanism

- There is no restriction on the user's caching technology selection, and the user can choose any caching technology.
- Internally supports object caching and associative caching, and in complex data structure queries, the two are organically combined on demand behind the scenes. The final effect presented to the user is a cache of arbitrary complex data structures, rather than a cache of simple objects.
- Automatically guarantees the data consistency of cache, just simply call Jimmer's API after receiving the database binlog push.
- The caching mechanism is 100% transparent to developers, and whether or not caching is used has no impact on business code.

:::tip
Although RDBMS has unparalleled expressive power, it has one obvious disadvantage: the performance of navigating and tracking other data according to the relationship is not ideal.

Associative caching can alleviate this problem to a large extent and make RDBMS even more powerful.
:::

### 3.4 Strongly typed SQL DSL more practical than native SQL
- Found typos and type matching errors at compile time.
- Strongly typed SQL DSL can be mixed with native SQL expressions at will, allowing the unique capabilities of specific database products to be used while unifying and abstracting different databases.
- Provides multi-table join operations that are more convenient and practical than native SQL at the cost of abandoning individual SQL writing ways that are almost impossible to use in actual projects.


