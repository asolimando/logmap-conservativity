# this is automatically generate makefile for KB

FILES= dl.res Taxonomy.Roles Taxonomy.DRoles Taxonomy.ORoles Taxonomy.log reasoning.log response.xml
all: sat

clean:
	for file in $(FILES); do \
		rm -f $$file $$file.diff; \
	done

KB_CONF = DataTypeTest.conf
KB = DataTypeTest.tbox

sat classify:
	FaCT++ $(KB_CONF)
