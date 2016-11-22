// Licensed to the .NET Foundation under one or more agreements.
// The .NET Foundation licenses this file to you under the MIT license.
// See the LICENSE file in the project root for more information.
// ============================================================
//
// CoreBindResult.cpp
// 

//
// Implements the CoreBindResult class
// ============================================================


#include "common.h"

#include "../binder/inc/assembly.hpp"

#ifndef FEATURE_FUSION
#ifndef DACCESS_COMPILE

STDMETHODIMP CoreBindResult::QueryInterface(REFIID   riid,
                                          void   **ppv)
{
    HRESULT hr = S_OK;

    if (ppv == NULL)
    {
        hr = E_POINTER;
    }    
    else
    {
        if ( IsEqualIID(riid, IID_IUnknown) )
        {
            AddRef();
            *ppv = static_cast<IUnknown *>(this);
        }
        else
        {
            *ppv = NULL;
            hr = E_NOINTERFACE;
        }
    }

    return hr;
}

STDMETHODIMP_(ULONG) CoreBindResult::AddRef()
{
    return InterlockedIncrement(&m_cRef);
}

STDMETHODIMP_(ULONG) CoreBindResult::Release()
{
    ULONG ulRef = InterlockedDecrement(&m_cRef);

    if (ulRef == 0) 
    {
        delete this;
    }

    return ulRef;
}


#endif  // DACCES_COMPILE
#endif // FEATURE_FUSION