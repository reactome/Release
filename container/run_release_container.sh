#! /bin/bash

RELEASE_NUMBER=$1

mkdir -p logs/uniprot_update
mkdir -p logs/orthopairs
mkdir -p logs/go_update
mkdir -p logs/chebi_update
mkdir -p logs/update_stable_ids
mkdir -p logs/myisam
mkdir -p logs/orthoinference
mkdir -p logs/simplified_database

touch logs/uniprot_update/uniprot.wiki
touch logs/uniprot_update/uniprot.err
touch logs/uniprot_update/uniprot.out
touch logs/uniprot_update/gk_central.dump.gz
touch logs/uniprot_update/reference_DNA_sequence_report.txt
touch logs/uniprot_update/sequence_uniprot_report.txt
touch logs/uniprot_update/duplicated_db_id.txt
touch logs/uniprot_update/trembl_to_update.acc
touch logs/orthopairs/compara.err
touch logs/orthopairs/compara.log
touch logs/go_update/ec_number.err
touch logs/go_update/ec_number.out
touch logs/go_update/go.out
touch logs/go_update/go.err
touch logs/go_update/go.wiki
touch logs/go_update/gk_central_after_uniprot_update.dump.gz
touch logs/chebi_update/improve_chebi_ids.out
touch logs/chebi_update/improve_chebi_ids.err
touch logs/chebi_update/chebi.wiki
touch logs/update_stable_ids/${RELEASE_NUMBER}.dump.gz
touch logs/update_stable_ids/gk_central_${RELEASE_NUMBER}_before_st_id.dump.gz
touch logs/update_stable_ids/gk_central.dump.gz
touch logs/update_stable_ids/test_slice_${RELEASE_NUMBER}_after_st_id.dump.gz
touch logs/update_stable_ids/test_slice_${RELEASE_NUMBER}.dump.gz
touch logs/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.err
touch logs/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.out
touch logs/update_stable_ids/update_stable_ids.err
touch logs/update_stable_ids/update_stable_ids.log
touch logs/update_stable_ids/pre_step_test_errors.log
touch logs/update_stable_ids/correct_stable_ids.log
touch logs/myisam/myisam.out
touch logs/myisam/myisam.err
touch logs/orthoinference/deleted_unused_pe_test_reactome_${RELEASE_NUMBER}.txt
touch logs/orthoinference/infer_events.err
touch logs/orthoinference/infer_events.log
touch logs/orthoinference/remove_unused_PE.log
touch logs/orthoinference/remove_unused_PE.err
touch logs/orthoinference/tweak_data_model.err
touch logs/orthoinference/tweak_data_model.log
touch logs/orthoinference/wrapper_ortho_inference.log
touch logs/orthoinference/wrapper_ortho_inference.err
touch logs/orthoinference/test_reactome_${RELEASE_NUMBER}_after_ortho.dump.gz
touch logs/orthoinference/normal_event_skip_list.txt
touch logs/orthoinference/simplified_database.out
touch logs/orthoinference/simplified_database.log
touch logs/orthoinference/simplified_database.err

docker run -it --name release_system --net reactome_release \
	-v $(pwd)/stable_id_mapping.stored_data:/release/scripts/release/generate_stable_ids_orthoinference/stable_id_mapping.stored_data \
	-v $(pwd)/Secrets.pm:/release/modules/GKB/Secrets.pm \
	-v $(pwd)/release-config.pm:/release/modules/GKB/Release/Config.pm \
	-v $(pwd)/logs/uniprot_update/uniprot.wiki:/release/scripts/release/uniprot_update/uniprot.wiki \
	-v $(pwd)/logs/uniprot_update/uniprot.err:/release/scripts/release/uniprot_update/uniprot.err \
	-v $(pwd)/logs/uniprot_update/uniprot.out:/release/scripts/release/uniprot_update/uniprot.out \
	-v $(pwd)/logs/uniprot_update/reference_DNA_sequence_report.txt:/release/scripts/release/uniprot_update/reference_DNA_sequence_report.txt \
	-v $(pwd)/logs/uniprot_update/sequence_uniprot_report.txt:/release/scripts/release/uniprot_update/sequence_uniprot_report.txt \
	-v $(pwd)/logs/uniprot_update/duplicated_db_id.txt:/release/scripts/release/uniprot_update/duplicated_db_id.txt \
	-v $(pwd)/logs/uniprot_update/trembl_to_update.acc:/release/scripts/release/uniprot_update/trembl_to_update.acc \
	-v $(pwd)/logs/orthopairs/$RELEASE_NUMBER:/release/scripts/release/orthopairs/$RELEASE_NUMBER \
	-v $(pwd)/logs/orthopairs/compara.err:/release/scripts/release/orthopairs/compara.err \
	-v $(pwd)/logs/orthopairs/compara.log:/release/scripts/release/orthopairs/compara.log \
	-v $(pwd)/logs/go_update/ec_number.err:/usr/local/gkb/scripts/release/go_update/ec_number.err:rw \
	-v $(pwd)/logs/go_update/ec_number.out:/usr/local/gkb/scripts/release/go_update/ec_number.out:rw \
	-v $(pwd)/logs/go_update/go.out:/usr/local/gkb/scripts/release/go_update/go.out:rw \
	-v $(pwd)/logs/go_update/go.err:/usr/local/gkb/scripts/release/go_update/go.err:rw \
	-v $(pwd)/logs/go_update/go.wiki:/usr/local/gkb/scripts/release/go_update/go.wiki:rw \
	-v $(pwd)/logs/chebi_update/improve_chebi_ids.out:/usr/local/gkb/scripts/release/chebi_update/improve_chebi_ids.out \
	-v $(pwd)/logs/chebi_update/improve_chebi_ids.err:/usr/local/gkb/scripts/release/chebi_update/improve_chebi_ids.err \
	-v $(pwd)/logs/chebi_update/chebi.wiki:/usr/local/gkb/scripts/release/chebi_update/chebi.wiki \
	-v $(pwd)/logs/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.err:/usr/local/gkb/scripts/release/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.err \
	-v $(pwd)/logs/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.out:/usr/local/gkb/scripts/release/update_stable_ids/generate_stable_ids_${RELEASE_NUM}.out \
	-v $(pwd)/logs/update_stable_ids/update_stable_ids.err:/usr/local/gkb/scripts/release/update_stable_ids/update_stable_ids.err \
	-v $(pwd)/logs/update_stable_ids/update_stable_ids.log:/usr/local/gkb/scripts/release/update_stable_ids/update_stable_ids.log \
	-v $(pwd)/logs/update_stable_ids/pre_step_test_errors.log:/usr/local/gkb/scripts/release/update_stable_ids/pre_step_test_errors.log \
	-v $(pwd)/logs/update_stable_ids/correct_stable_ids.log:/usr/local/gkb/scripts/release/update_stable_ids/correct_stable_ids.log \
	-v $(pwd)/logs/myisam/myisam.out:/usr/local/gkb/scripts/release/myisam/myisam.out \
	-v $(pwd)/logs/myisam/myisam.err:/usr/local/gkb/scripts/release/myisam/myisam.err \
	-v $(pwd)/logs/orthoinference/deleted_unused_pe_test_reactome_${RELEASE_NUMBER}.txt:/usr/local/gkb/scripts/release/orthoinference/deleted_unused_pe_test_reactome_${RELEASE_NUMBER}.txt \
	-v $(pwd)/logs/orthoinference/infer_events.err:/usr/local/gkb/scripts/release/orthoinference/infer_events.err \
	-v $(pwd)/logs/orthoinference/infer_events.log:/usr/local/gkb/scripts/release/orthoinference/infer_events.log \
	-v $(pwd)/logs/orthoinference/remove_unused_PE.log:/usr/local/gkb/scripts/release/orthoinference/remove_unused_PE.log \
	-v $(pwd)/logs/orthoinference/remove_unused_PE.err:/usr/local/gkb/scripts/release/orthoinference/remove_unused_PE.err \
	-v $(pwd)/logs/orthoinference/tweak_data_model.err:/usr/local/gkb/scripts/release/orthoinference/tweak_data_model.err \
	-v $(pwd)/logs/orthoinference/tweak_data_model.log:/usr/local/gkb/scripts/release/orthoinference/tweak_data_model.log \
	-v $(pwd)/logs/orthoinference/wrapper_ortho_inference.log:/usr/local/gkb/scripts/release/orthoinference/wrapper_ortho_inference.log \
	-v $(pwd)/logs/orthoinference/wrapper_ortho_inference.err:/usr/local/gkb/scripts/release/orthoinference/wrapper_ortho_inference.err \
	-v $(pwd)/logs/orthoinference/normal_event_skip_list.txt:/usr/local/gkb/scripts/release/orthoinference/normal_event_skip_list.txt \
	-v $(pwd)/logs/orthoinference/simplified_database.out:/usr/local/gkb/scripts/release/simplified_database/simplified_database.out \
	-v $(pwd)/logs/orthoinference/simplified_database.log:/usr/local/gkb/scripts/release/simplified_database/simplified_database.log \
	-v $(pwd)/logs/orthoinference/simplified_database.err:/usr/local/gkb/scripts/release/simplified_database/simplified_database.err \
	-v $(pwd)/nfs_backup:/nfs \
	reactome-release /bin/bash

# TODO: Figure out a nice way to get the dump/backup files out of the container
