# HFQL SQL Syntax: Where and Having

HFQL provides two mechanisms for selecting specific data from the database: _WHERE_ and _HAVING_. These mechanisms both work similarly to standard SQL but also have important differences.

# WHERE Clauses

The HFQL _WHERE_ keyword is used to apply standard FHIR search expressions to restrict the data being returned. Unlike SQL _WHERE_ statements, HFQL WHERE statements operate on FHIR Search Parameters instead of on columns.

The basic syntax for search clauses is: [param_name] = '[param_value'. 

Both _param_name_ and _param_value_ appear exactly as they would within a [FHIR search URL](https://smilecdr.com/docs/fhir_standard/fhir_search_queries.html). 

A simple example follows:

```sql
SELECT name.family, name.given
FROM Patient
WHERE identifier = 'http://system|value'
```

Modifiers and other advanced search parameters are also supported. The following example shows a `_has` parameter being used to return the IDs of patients having an Observation generated by a device with the given identifier.

```sql
SELECT
   id
FROM
   Patient
WHERE
   _has:Observation:subject:device.identifier = 'http://foo|1234'
```

Values can include prefix comparators in HFQL expressions just as they can in search URLs. They can also be combined using the _AND_ keyword.

```sql
SELECT id
FROM Observation
WHERE 
    code = 'http://loinc.org|29463-7'
    AND
    value-quantity = 'lt5|http://unitsofmeasure.org|kg'
```

# HAVING Clauses

HFQL statements can also use the _HAVING_ keyword to restrict the data that is returned. While the _WHERE_ keyword uses database search indexes to determine whether data should be included, the _HAVING_ keyword filters data after it has been loaded from the database.

A _HAVING_ clause uses a [FHIRPath Expression](https://smilecdr.com/docs/fhir_standard/fhirpath_expressions.html), and can filter based on any element in any resource. 

_HAVING_ clauses have the advantage that they are not limited to existing SearchParameter indexes. However, they have the disadvantage that they need to load every candidate resource in order to apply FHIRPath tests to it. This means that using _HAVING_ will often be **much slower** than using _WHERE_ clauses. If at all possible, you should use _WHERE_ clauses first and then add additional _HAVING_ clauses to further refine your criteria.

The following example shows a search for all cardiology note Observations (LOINC code 29463-7) where the string "running" appears anywhere in the text of the note.

```sql
SELECT id
FROM Observation
WHERE code = 'http://loinc.org|34752-6'
HAVING
   value.ofType(string).lower().contains('running')
```

FHIRPath expressions can contain comparisons too. The following example shows a search for any weight assessment Observations (LOINC code 29463-7) with a value below 10 kg.

```sql
SELECT
   id,
   value.ofType(Quantity).value,
   value.ofType(Quantity).system,
   value.ofType(Quantity).code
FROM Observation
WHERE
   code = 'http://loinc.org|29463-7'
HAVING
   value.ofType(Quantity).value < 10
```