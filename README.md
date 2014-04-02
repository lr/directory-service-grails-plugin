DirectoryService
===========

DirectoryService is a [Grails](http://grails.org/) [plugin](http://grails.org/plugins/) that allows you to interact with a v3-compliant LDAP server with minimal effort. It is built on top of the [UnboundID](http://www.unboundid.com/) [LDAP SDK](http://www.unboundid.com/products/ldap-sdk/) ([UnboundID LDAP SDK Java docs](https://www.unboundid.com/products/ldap-sdk/docs/javadoc/index.html)).

DirectoryService is made up of two key classes, the DirectoryService Grails Service class, and the DirectoryServiceEntry class, the class to which results are mapped. See the official documentation for more information about each of these classes.

Official documentation can be found at http://lr.github.com/directory-service/.

## Install

To install the plugin, add the following to the `plugins` section of BuildConfig.groovy:

```groovy
runtime ":directory-service:0.10.0"
```

## Configure

DirectoryService requires two Maps to be configured:

* `grails.plugins.directoryservice.sources`
* `grails.plugins.directoryservice.dit`

Since both of these are Groovy Map objects, they must be put in a `.groovy` file (like `grails-app/conf/Config.groovy` or some other ConfigSlurper file), and can not go in a `.properties` file.

### Directory Sources Map

The `grails.plugins.directoryservice.sources` Map is called sources for a reason: You can have more than one directory source. Where I work we have a main directory service, 5 AD domains, and an AD Global Catalog; that's 7 sources! So, this plugin is designed to work with all of your directory sources at once. This is very much modeled after that multiple `dataSource` feature of GORM.

The `grails.plugins.directoryservice.sources` uses the following syntax:

```groovy
ds.sources = [
    'source name':[
        address: String // Single address, multiple separated by a ",".
        port: String, // Single port, or multiple separated by a ",". Must match 'address' number.
        useSSL: boolean,
        trustSSLCert: boolean,
        bindDN: String,
        bindPassword: String,
        useConnectionPool: boolean, // Optional
        initialConnections: integer, // Optional
        maxConnections: integer // Optional
    ],
]
```

So, if you have two sources, like an "enterprise directory", and "AD Global Catalog", your `grails.plugins.directoryservice.sources` might look like this:

```groovy
grails.plugins.directoryservice.sources = [
    'directory':[
        address: 'ldap.someu.edu',
        port: '636',
        useSSL: true,
        trustSSLCert: true
        bindDN: 'cn=some bind DN',
        bindPassword: 'MyPassw0rd!'
        useConnectionPool: true,
        initialConnections: 2,
        maxConnections: 5
    ],
    'adGC':[
        address: 'adgc.someu.edu',
        port: '3269',
        useSSL: true,
        trustSSLCert: true
        bindDN: 'cn=another bind DN',
        bindPassword: 'An0therPassw0rd!'
    ],
]
```

You will see how these sources are referenced in the DIT Map, below.

### Directory DIT Map

The `grails.plugins.directoryservice.dit` is another Map that is literally a map of your directory. Of course, you only need to map out the areas of your directory that you want to use. For instance, for the above two sources, we might have a few sections that we want to define: `people`, `departments`, `accounts`, and `groups` (at least one type of group).

The `ds.dit` uses the following syntax:

```groovy
grails.plugins.directoryservice.dit = [
    'base DN of the branch':[
        singular: String,
        plural: String,
        rdnAttribute: String,
        source: String,
        attributes: ['*'] // Optional, and default is *. See examples below.
    ]
]
```

So, let's define our four branches and relate them to the two sources we defined in `grails.plugins.directoryservice.sources`:

```groovy
grails.plugins.directoryservice.dit = [
    'ou=people,dc=someu,dc=edu':[
        singular: 'person',
        plural: 'people',
        rdnAttribute: 'uid',
        source:'directory',
        attributes: ['*', 'isMemberOf']
    ],
    'ou=departments,dc=someu,dc=edu':[
        singular: 'department',
        plural: 'departments',
        rdnAttribute: 'ou',
        source: 'directory',
        attributes: ['*', '+']
    ],
    'ou=groups,dc=someu,dc=edu':[
        singular: 'directoryGroup',
        plural: 'directoryGroups',
        rdnAttribute: 'cn',
        source: 'directory'
    ],
    'ou=accounts,dc=someu,dc=edu':[
        singular: 'account',
        plural: 'accounts',
        rdnAttribute: 'cn',
        source: 'adGC',
        attributes: ['sAMAccountName', 'employeeID', 'givenName', 'sn', 'memberOf']
    ],
    'ou=Groups,dc=someu,dc=edu':[
        singular: 'adGroup',
        plural: 'adGroups',
        rdnAttribute: 'cn',
        source: 'adGC'
    ]
]
```

As you can see above, we have two `ou=groups,dc=someu,dc=edu` branches, one in our enterprise directory, and one in AD. So, we get around this by making one of them -- in this case the AD groups OU -- have an uppercase "G". This works because Map keys are case-sensitive, but thankfully, directories usually do not care about case when specifying a base!

See the Usage section for details on how to perform searches using the information you configured above.

## Usage

### Add DirectoryService

DirectoryService is just like any other [Grails Service](http://grails.org/doc/latest/guide/single.html#services), and can be added to your Controller and Service classes the way any other service is added.

```groovy
def directoryService
```

or

```groovy
DirectoryService directoryService
```

### Search (Find)

As of this version, DirectoryService only supports searching LDAP. Add, update, and delete will be added in forthcoming releases.

Also, unlike GORM, DirectoryService currently only supports `find<singular|plural>Where` methods, not the more sophisticated `<Object>.findBySomeattributeAndAnotherattribute` methods.

At this point you will see why we use a singular and plural name for each branch defined in `directoryService.dit`.

#### Find a Person

Using the example `dit` above, to find a person, you would do the following:

```groovy
def person = directoryService.findPersonWhere(uid:'12345')
def person = directoryService.findPersonWhere(sn:'rockwell', givenName:'lucas')
```

Since `uid` is the RDN attribute for the `ou=people` branch, the first search above is pretty much guaranteed to find just one person. Whereas the second could potentially fine more then one person. However, in both cases, DirectoryService will only return at most <strong>one</strong> record (which will be a DirectoryServiceEntry object). It only returns one because you are using the singular `findPersonWhere`, and semantically that means you only want one person.

This is similar to the GORM `get()` method.

#### Find People

To find more than one person, you would use the plural form that you defined in `dit`. For instance, for your people branch, you would use `findPeopleWhere`:

```groovy
def people = directoryService.findPeopleWhere(sn:'rockwell')
def people = directoryService.findPeopleWhere(departmentNumber:'12345')
```

Or multiple key/value pairs:

```groovy
def people = directoryService.findPeopleWhere(sn:'rockwell', st:'ca')
```

If you are searching for more than one of something, you can sort the result by providing an optional Map with a single key `sort` and the attribute by which you wish to sort:

```groovy
def people = directoryService.findPeopleWhere('sn':'rockwell', [sort:'cn'])
```

Or:

```groovy
def people = directoryService.findPeopleWhere(sn:'rockwell', st:'ca', [sort:'cn'])
```

Of course, the attribute you specify in the sort must be included in the return params in the dit entry (and the bind you are using must also have access to the attribute). Also, at this time, you can only specify one attribute on which to sort. If you want to be able to sort on more than one attribute, please let me know.

### Update (Save)

To be GORM-like, update is implemented as `save`, but unlike GORM, it is implemented on the DirectoryService object, and not the entry object itself. Below is an example of finding, updating, and then saving an entry:

```groovy
def person = directoryService.findPersonWhere('employeeNumber':'23576')
person.sn = 'Franklin-Jackson'
person.cn = ['Sally Franklin-Jackson', 'Franklin-Jackson, Sally']
person.displayName = 'Sally Franklin-Jackson'
person.mail = 'sally.franklin-jackson@somu.edu'
directoryService.save(person)
```

See the official docs and reference guide for more details about saving objects back to the directory.

### Add (Create)

Not implemented yet.

### Delete

Although not officially implemented, you can do the following to delete an object:

* Call `directoryService.connection(baseDN)?.delete(DN of object to delete)` and pass it the value of one of the base DNs for the `grails.plugins.directoryservice.dit` map.

So, using the `grails.plugins.directoryservice.dit` map above, to delete a person, you would do the following:

`directoryService.connection('ou=people,dc=someu,dc=edu')?.delete('uid=12345,ou=people,dc=someu,dc=edu')`

Or, if you have the object that you want to delete:

```
def person = directoryService.findPersonWhere(uid:'12345')
directoryService.connection('ou=people,dc=someu,dc=edu')?.delete(person.dn)
```

See the [UnboundID LDAP SDK Java docs](https://www.unboundid.com/products/ldap-sdk/docs/javadoc/index.html) for more info about the capabilities of the SDK.

## License

DirectoryService is licensed under the terms of the [Apache License, Version 2.0 (LICENSE-2.0)](http://www.apache.org/licenses/LICENSE-2.0). The UnboundID LDAP SDK is licensed under the terms of three different licenses. Please see the [UnboundID LDAP SDK for Java docs page](https://www.unboundid.com/products/ldap-sdk/docs/javadoc/index.html) for more information.