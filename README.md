DirectoryService
===========

DirectoryService is a [Grails](http://grails.org/) [plugin](http://grails.org/plugins/) that allows you to interact with a v3-compliant LDAP server with minimal effort. It is built on top of the [UnboundID](http://www.unboundid.com/) [LDAP SDK](http://www.unboundid.com/products/ldap-sdk/).

## Install

To install the DirectoryService plugin, add the following to the `plugins` section of `grails-app/conf/BuildConfig.groovy`

    runtime ...

## Configure

DirectoryService requires two Maps to be configured:

* `ds.sources`
* `ds.dit`

Since both of these are Groovy Map objects, they must be put in a `.groovy` file, and can not go in a `.properties` file.

### Directory Sources Map

The following LDAP parameters are required. No defaults are assumed. If you do not provide these parameters, you will get an exception when you try to connect to your LDAP.

The connection parameters below can go into either `grails-app/conf/Config.groovy`, an external `.groovy` config file, or an external `.properties` file.

<pre>
    
</pre>

### Directory DIT Map

Since the following is a Groovy Map object, you must put it in either `grails-app/conf/Config.groovy`, or an external `.groovy` file. In other words, the following can *not* go into a `.properties` file.

Since LDAP is a tree of objects, you simply need to tell DirectoryService the layout of your tree, and the names of the elements you want to use for each branch of the tree. Unlike GORM, DirectoryService uses language constructs to determine if you want one or all matches for a search. For instance, if a branch of your LDAP tree is named `ou=people`, you would configure DirectoryService to know this branch as both `person` and `people`. This will allow you to do searches like `findPersonWhere` and `findPeopleWhere` which will find one or all people, respectively, which match your search.

<strong>Syntax:</strong>

<pre>
    directoryService.dit = [
        'base DN of a branch (like ou=people)':[
            'singular': string,
            'plural': string,
            'rdnAttribute': string
        ]
    ]
</pre>

<strong>Example:</strong>

<pre>
    directoryService.dit = [
        'ou=people,dc=ucsf,dc=edu':[
            'singular': 'person',
            'plural': 'people',
            'rdnAttribute': 'uid'
        ],
        'ou=departments,dc=ucsf,dc=edu':[
            'singular': 'department',
            'plural': 'departments',
            'rdnAttribute': 'ou'
        ],
        'ou=groups,dc=ucsf,dc=edu':[
            'singular': 'group',
            'plural': 'groups',
            'rdnAttribute': 'cn'
        ]
    ]
</pre>

See the Usage section for details on how to perform searches using the information you configured above.

## Usage

### Add DirectoryService

DirectoryService is just like any other [Grails Service](http://grails.org/doc/latest/guide/single.html#services), and can be added to your Controller and Service classes the way any other service is added.

<pre>
    def directoryService
</pre>

or

<pre>
    DirectoryService directoryService
</pre>

### Search (Find)

As of this version, DirectoryService only supports searching LDAP. Add, update, and delete will be added in forthcoming releases.

Also, unlike GORM, DirectoryService currently only supports `find*Where` methods, not the more sophisticated `find*BySomeattributeAndAnotherattribute`.

At this point you will see why we use a singular and plural name for each branch defined in `directoryService.dit`.

#### Find a Person

Using the example `directoryService.dit` above, to find a person, you would do the following:

<pre>
    def person = directoryService.findPersonWhere('uid':'12345')
    
    def person = directoryService.findPersonWhere('sn':'rockwell', 'givenName':'lucas')
</pre>

Since `uid` is the RDN attribute for the `ou=people` branch, the first search above is pretty much guaranteed to find just one person. Whereas the second could potentially fine more then one person. However, in both cases, DirectoryService will only return at most <strong>one</strong> record (which will be an DirectoryServiceEntry object). It only returns one because you are using the singular `findPersonWhere`, and semantically that means you only want a person.

This is similar to the GORM `get()` method, however, DirectoryService also implements `get()`;

<pre>
    
</pre>

To find more than one person, you would use the plural form, `findPeopleWhere`:

<pre>
    def people = directoryService.findPeopleWhere('sn':'rockwell')
    
    def people = directoryService.findPeopleWhere('departmentNumber':'12345')
</pre>


### Add (Create)

Not implemented yet.

### Update (Save)

Not implemented yet.

### Delete

Not implemented yet.


## License

DirectoryService is licensed under the terms of the [Apache License, Version 2.0 (LICENSE-2.0)](http://www.apache.org/licenses/LICENSE-2.0). The UnboundID LDAP SDK is licensed under the terms of three different licenses. Please see the [UnboundID LDAP SDK for Java docs page](http://www.unboundid.com/products/ldap-sdk/docs/) for more information.