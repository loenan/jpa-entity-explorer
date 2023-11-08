# jpa-entity-explorer

A small tool to explore and discover the entity graph in an existing project.

## Installation

Maven:
```xml
<dependency>
    <groupId>loenan-tools</groupId>
    <artifactId>jpa-entity-explorer</artifactId>
    <version>${jpa-entity-explorer.version}</version>
</dependency>
```

## Usage

Once the Maven dependency is declared in the project you want to explore, run this command:

```shell
java $JAVA_OPTS com.loenan.tools.jpa.entityexplorer.EntityExplorer com.myproject.jpapackage
```

where `com.myproject.jpapackage` is the package name where to scan JPA entities in your project. 
You can change it, or provide several ones (as program arguments).

It will write a description of the JPA entity graph to the standard output:

- Each entity is described with a first line about the entity itself (name, maybe parent entity name, JPA annotation summary).
  Example:
```
## Book (@Entity, @Table("book"))
```

- Each property of an entity is described on a subsequent line (name, type, JPA annotation summary). 
  Example:
```
    title: String (@Column(!nullable, "title"))
```

- A property is considered as a relationship to another entity if it is annotated with a JPA annotation 
  `OneToOne`, `OneToMany`, `ManyToOne`, or `ManyToMany`.
  Outgoing relationships are listed first (before other properties). 
  If the target entity is referenced by only one relationship, that entity will be described just after,
  with an incremented indentation level.
  Example:
```
    --[publisher]--> Publisher (@ManyToOne(fetch=LAZY, cascade=[ALL]))
        ## ManyToOne (@Entity, @Table("publisher"))
```

- When a relationship is bidirectional (using `mappedBy` attribute), the direction tagged with the `mappedBy` attribute
  is considered as the main one. The inverse direction is described with simple properties, like this:
```
    <--[book]-- Book (@ManyToOne(!optional))
```

- When a target entity is referenced by many relationships, an ellipsis is displayed as a reference to the differed description:
```
    --[authors]--> <List> BookAuthor (@OneToMany(mappedBy="book"))
        ...
```
  then, the target entity description will remind the incoming relationships:
```
## BookAuthor (@Entity, @Table("book_author"))
    ...[books]... Author
    ...[authors]... Book
```

- In the case of a cycle between entities, one relationship in the cycle will be ignored. 
  Ignored relationships are described in the list of simple properties, but keeping the syntax of the relationship description.

- In the case of an entity class hierarchy, the subclasses will be described under their base class, 
  at the same indentation level, but with a different prefix (`\\` instead of `##`). 
  The base class of a subclass is also reminded on the entity description line.
  Example:
```
## Person (@DiscriminatorColumn(discriminatorType=STRING, "type"), @DiscriminatorValue("0"), @Entity, @Inheritance, @Table("person"))
    id: Long (@GeneratedValue(strategy=IDENTITY), @Id)
    firstName: String
    lastName: String
\\ Author: Person (@DiscriminatorValue("5"), @Entity)
    --[publisher]--> Publisher (@ManyToOne)
```

- The root entity descriptions are sorted by the number of referenced entities (considering the sub-graph),
  the largest one first, to consider them as top entities.
