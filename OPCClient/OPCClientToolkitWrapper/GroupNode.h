#pragma once

#include "OPCItem.h"
#include "Utils.h"
#include <assert.h>
#include "TransactionCompleteHandler.h"

#include <pantheios/pantheios.hpp>
#include <pantheios/inserters/integer.hpp>

using namespace pantheios;
using namespace std;
using namespace Utils;

extern TransactionCompleteHandler transactionHandler;

struct GroupNode
	{

	private:
		char* m_pGroupName;
		COPCGroup* m_pGroup;
		CAtlMap<CString , COPCItem *> m_items;
		CAtlMap<CString, int> m_itemsDataTypes;

		int GetItemDataType(COPCItem* pItem)
		{
			if(pItem == NULL)
			{				
				RecordError("GetItemDataType - received [null] input");
				assert(false);
				return -1;
			}

			return pItem->getDataType();
		}

		bool WriteItem(const char* const pItemName, const char* const pValue, const bool isAsync)
		{
			log_NOTICE("writing item [", pItemName,"]");

			COPCItem* pItem = NULL;
			if(m_items.Lookup(CString(pItemName), pItem))
			{
				log_NOTICE("got item [", pItemName,"]");
				VARTYPE vt = m_itemsDataTypes[CString(pItemName)];
				if(VT_EMPTY != vt)
				{
					log_NOTICE("got item type [", pantheios::integer(vt),"]");
					_variant_t varValue(pValue);
					if(S_OK == VariantChangeType(&varValue.GetVARIANT(), &varValue.GetVARIANT(), VARIANT_ALPHABOOL, vt))
					{
						if(isAsync)
						{
							log_NOTICE("starting async write");
							CTransaction* pTransaction = pItem->writeAsynch(varValue, &transactionHandler);
							log_NOTICE("completed async write: item [",pItemName,"] transaction id [",pantheios::integer(pTransaction->getCancelId()),"]");
						}
						else
						{
							pItem->writeSync(varValue);
						}
						
						log_NOTICE("WriteItem: successfully requested write item [", pItemName,"] value [", pValue,"] async [",(isAsync?"T":"F"),"]");
						return true;
					}
				}
			}

			RecordError("WriteItem: failed to write item [%s] value [%s] async [%s]", pItemName, pValue, (isAsync?"T":"F"));
			return false;
		}

	public:

		GroupNode(const char* const pGroupName, COPCGroup* pGroup):m_pGroupName(_strdup(pGroupName)), m_pGroup(pGroup)
		{
			m_items.InitHashTable(257);
			m_items.RemoveAll();

			m_itemsDataTypes.InitHashTable(257);
			m_itemsDataTypes.RemoveAll();
		};

		virtual ~GroupNode()
		{
			log_NOTICE("Deleting group node [", m_pGroupName,"]");

			m_pGroup->disableAsynch();

			delete m_pGroup;
			free(m_pGroupName); 
		}

		void AddItem(const char* const pItemName)
		{
			log_NOTICE("GroupNode [", m_pGroupName,"] adding item [", pItemName,"]...");
			COPCItem* pItem = m_pGroup->addItem(CString(pItemName), true);
						
			m_items[CString(pItemName)] = pItem;
			m_itemsDataTypes[CString(pItemName)] = GetItemDataType(pItem);

			log_NOTICE("Added item to group - name [", pItemName,"] type [", pantheios::integer(m_itemsDataTypes[CString(pItemName)]),"]");
		};

		bool ReadItemSync(const char* const pItemName, OPCItemData& itemData)
		{
			log_NOTICE("ReadItemSync: GroupNode [", m_pGroupName,"] reading item [", pItemName,"] (total number of group items [",pantheios::integer(m_items.GetCount()),"])");

			COPCItem* pItem = NULL;
			if(m_items.Lookup(CString(pItemName), pItem))
			{
				log_NOTICE("ReadItemSync: GroupNode [", m_pGroupName,"] found item [", pItemName,"]");
	  			pItem->readSync(itemData, OPC_DS_DEVICE);
				return true;
			}

			RecordError("ReadItemSync: failed to read item [%s]", pItemName);
			return false;
		}

		const DWORD GetItemAccessRights(const char* const pItemName)
		{
			log_NOTICE("GetItemAccessRights: GroupNode [", m_pGroupName,"] item [", pItemName,"] (total number of group items [",pantheios::integer(m_items.GetCount()),"])");

			COPCItem* pItem = NULL;
			if(m_items.Lookup(CString(pItemName), pItem))
			{
				log_NOTICE("GetItemAccessRights: GroupNode [", m_pGroupName,"] found item [", pItemName,"]");
				return pItem->getAccessRights();
			}

			RecordError("GetItemAccessRights: failed to find item [%s]", pItemName);
			return 0;
		}

		const int GetItemDatatype(const char* const pItemName)
		{
			log_NOTICE("GetItemDatatype: GroupNode [", m_pGroupName,"] item [", pItemName,"] (total number of group items [",pantheios::integer(m_items.GetCount()),"])");
			
			COPCItem* pItem = NULL;
			if(m_items.Lookup(CString(pItemName), pItem))
			{
				log_NOTICE("GetItemDatatype: GroupNode [", m_pGroupName,"] found item [", pItemName,"]");
				return pItem->getDataType();
			}

			RecordError("GetItemDatatype: failed to find item [%s]", pItemName);
			return 0;
		}

		bool ReadItemAsync(const char* const pItemName)
		{
			log_NOTICE("ReadItemAsync: GroupNode [", m_pGroupName,"] reading item [", pItemName,"]");

			COPCItem* pItem = NULL;
			if(m_items.Lookup(CString(pItemName), pItem))
			{
				log_NOTICE("ReadItemAsync: GroupNode [", m_pGroupName,"] found item [", pItemName,"]");
				
				CTransaction* pTransaction = pItem->readAsynch(&transactionHandler);
				char idBuff[64];
				memset(idBuff, 64, 0);
				sprintf_s(idBuff, 64, "%d", pTransaction->getCancelId());
				log_NOTICE("ReadItemAsync cancel id [", idBuff,"]");


				log_NOTICE("ReadItemAsync: async read requested for GroupNode [", m_pGroupName,"] translating item [", pItemName,"]");
				return true;
			}

			RecordError("ReadItemAsync: failed to read item [%s]", pItemName);
			return false;
		}

		bool WriteItemAsync(const char* const pItemName, const char* const pValue)
		{
			return WriteItem(pItemName, pValue, true);
		}

		bool WriteItemSync(const char* const pItemName, const char* const pValue)
		{
			return WriteItem(pItemName, pValue, false);
		}
	};