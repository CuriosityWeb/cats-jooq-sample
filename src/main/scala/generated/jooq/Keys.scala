/*
 * This file is generated by jOOQ.
 */
package generated.jooq


import generated.jooq.tables.Users
import generated.jooq.tables.records.UsersRecord

import org.jooq.TableField
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal


/**
 * A class modelling foreign key relationships and constraints of tables in the
 * default schema.
 */
object Keys {

  // -------------------------------------------------------------------------
  // UNIQUE and PRIMARY KEY definitions
  // -------------------------------------------------------------------------

  val CONSTRAINT_4: UniqueKey[UsersRecord] = Internal.createUniqueKey(Users.USERS, DSL.name("CONSTRAINT_4"), Array(Users.USERS.EMAIL).asInstanceOf[Array[TableField[UsersRecord, _] ] ], true)
}
